package world.isnap.filmpostcard.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import world.isnap.filmpostcard.dto.*;
import world.isnap.filmpostcard.service.AIService;
import world.isnap.filmpostcard.service.DownloadService;
import world.isnap.filmpostcard.service.FileStorageService;
import world.isnap.filmpostcard.service.OrderService;
import world.isnap.filmpostcard.service.PhotoService;
import world.isnap.filmpostcard.service.PostcardService;
import world.isnap.filmpostcard.service.StorageQuotaService;
import world.isnap.filmpostcard.service.UserService;
import world.isnap.filmpostcard.util.JwtUtil;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PostcardController {
    
    private final PostcardService postcardService;
    private final FileStorageService fileStorageService;
    private final AIService aiService;
    private final OrderService orderService;
    private final DownloadService downloadService;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final StorageQuotaService storageQuotaService;
    private final PhotoService photoService;
    
    // Cache to prevent duplicate uploads (key: username:filesize:filename, value: response)
    private final ConcurrentHashMap<String, CachedUploadResponse> uploadCache = new ConcurrentHashMap<>();
    
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class CachedUploadResponse {
        private ImageUploadResponse response;
        private long timestamp;
    }
    
    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "type", defaultValue = "photo") String fileType,
            @RequestParam(value = "albumId", required = false) String albumId,
            @RequestParam(value = "idempotencyKey", required = false) String idempotencyKey,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Extract and validate JWT token
            String username = extractUsernameFromToken(authHeader);
            if (username == null) {
                return ResponseEntity.status(401)
                        .body(Map.of("error", "Unauthorized", "message", "Valid JWT token required"));
            }
            
            // Validate image
            if (image.isEmpty()) {
                throw new RuntimeException("Image file is empty");
            }
            
            // Check file size (30MB = 30 * 1024 * 1024 bytes)
            long maxFileSize = 30 * 1024 * 1024;
            if (image.getSize() > maxFileSize) {
                throw new RuntimeException("File size exceeds 30MB limit");
            }
            
            String contentType = image.getContentType();
            if (contentType == null || 
                (!contentType.equals("image/jpeg") && !contentType.equals("image/jpg"))) {
                throw new RuntimeException("Invalid file type. Only JPG/JPEG images are allowed");
            }
            
            // Determine file type from parameter
            FileStorageService.FileType type = switch (fileType.toLowerCase()) {
                case "avatar" -> FileStorageService.FileType.AVATAR;
                case "photo" -> FileStorageService.FileType.PHOTO;
                case "postcard" -> FileStorageService.FileType.POSTCARD;
                default -> FileStorageService.FileType.PHOTO; // Default to photo
            };

            // Get user entity and check quota
            world.isnap.filmpostcard.entity.User user = userService.getUserEntity(username);
            storageQuotaService.validateUpload(user, image.getSize());
            
            // Store file in user-specific directory with type subdirectory
            FileStorageService.StoredFile storedFile = fileStorageService.storeFileWithSize(image, username, type);
            String imageUrl = "/api/images/" + storedFile.getRelativePath();
            String imageUrlThumb = storedFile.getRelativePathThumb() != null ? "/api/images/" + storedFile.getRelativePathThumb() : null;
            String imageUrlMedium = storedFile.getRelativePathMedium() != null ? "/api/images/" + storedFile.getRelativePathMedium() : null;
            
            // Update user's storage usage
            storageQuotaService.incrementStorage(user, storedFile.getFileSize());
            
            // If this is a photo type and albumId is provided, create Photo record
            String photoId = storedFile.getRelativePath();
            if (type == FileStorageService.FileType.PHOTO) {
                log.info("Creating photo record for user: {} with albumId: {}", username, albumId);
                PhotoUploadRequest photoRequest = PhotoUploadRequest.builder()
                        .imageUrl(imageUrl)
                        .imageUrlThumb(imageUrlThumb)    // ✅ 添加缩略图URL
                        .imageUrlMedium(imageUrlMedium)  // ✅ 添加中等尺寸URL
                        .albumId(albumId)
                        .build();
                
                try {
                    PhotoResponse photoResponse = photoService.uploadPhoto(username, photoRequest);
                    photoId = photoResponse.getId();
                    log.info("Photo record created: {} in album: {} (thumb: {}, medium: {})", 
                            photoId, albumId, imageUrlThumb, imageUrlMedium);
                } catch (RuntimeException e) {
                    log.error("Failed to create photo record with album: {}", albumId, e);
                    // Re-throw the exception to properly handle the error
                    throw e;
                }
            }
            
            ImageUploadResponse response = ImageUploadResponse.builder()
                    .id(photoId)
                    .url(imageUrl)
                    .urlThumb(imageUrlThumb)
                    .urlMedium(imageUrlMedium)
                    .filename(storedFile.getRelativePath())
                    .fileSize(storedFile.getFileSize())
                    .build();
            
            log.info("Image uploaded successfully: {} (type: {}) for user: {}", storedFile.getRelativePath(), type, username);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Error uploading image", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Internal Server Error", "message", "Failed to upload image"));
        } catch (RuntimeException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Bad Request", "message", e.getMessage()));
        }
    }
    
    /**
     * Extract username from JWT token in Authorization header
     * @param authHeader Authorization header (Bearer token)
     * @return username if valid, null otherwise
     */
    private String extractUsernameFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header");
            return null;
        }
        
        String token = authHeader.substring(7); // Remove "Bearer " prefix
        
        if (!jwtUtil.validateToken(token)) {
            log.warn("Invalid JWT token");
            return null;
        }
        
        if (jwtUtil.isTokenExpired(token)) {
            log.warn("Expired JWT token");
            return null;
        }
        
        String username = jwtUtil.getUsernameFromToken(token);
        if (username == null) {
            log.warn("Unable to extract username from token");
            return null;
        }
        
        log.debug("Authenticated user: {}", username);
        return username;
    }
    
    /**
     * Clean up old cache entries to prevent memory leaks
     */
    private void cleanupOldCacheEntries() {
        long currentTime = System.currentTimeMillis();
        long maxAge = TimeUnit.SECONDS.toMillis(30);
        
        uploadCache.entrySet().removeIf(entry -> {
            long age = currentTime - entry.getValue().getTimestamp();
            return age > maxAge;
        });
    }
    
    @PostMapping("/postcards")
    public ResponseEntity<PostcardResponse> createPostcard(
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "imageFilename", required = false) String imageFilename,
            @RequestParam(value = "text", required = false) String textContent,
            @RequestParam(value = "templateType", required = false) String templateType,
            @RequestParam(value = "qrUrl", required = false) String qrUrl,
            @RequestParam(value = "username", required = false) String username) {
        try {
            PostcardResponse response;
            
            // Support two modes: direct upload OR use existing image
            if (image != null && !image.isEmpty()) {
                // Mode 1: Upload new image directly
                response = postcardService.createPostcard(image, textContent, templateType, qrUrl, username);
            } else if (imageFilename != null && !imageFilename.isEmpty()) {
                // Mode 2: Use already uploaded image
                response = postcardService.createPostcardFromImage(imageFilename, textContent, templateType, qrUrl, username);
            } else {
                throw new RuntimeException("Either image or imageFilename must be provided");
            }
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Error creating postcard", e);
            return ResponseEntity.internalServerError().build();
        } catch (RuntimeException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/postcards/{id}")
    public ResponseEntity<PostcardResponse> getPostcard(@PathVariable Long id) {
        try {
            PostcardResponse response = postcardService.getPostcard(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Postcard not found: {}", id);
            return ResponseEntity.notFound().build();
        }
    }
    
    @PutMapping("/postcards/{id}")
    public ResponseEntity<PostcardResponse> updatePostcard(
            @PathVariable Long id,
            @RequestBody UpdatePostcardRequest request) {
        try {
            PostcardResponse response = postcardService.updatePostcard(
                    id,
                    request.getTextContent(),
                    request.getTemplateType(),
                    request.getQrUrl()
            );
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error updating postcard: {}", id, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/postcards")
    public ResponseEntity<List<PostcardResponse>> getAllPostcards() {
        List<PostcardResponse> postcards = postcardService.getAllPostcards();
        return ResponseEntity.ok(postcards);
    }
    
    @DeleteMapping("/postcards/{id}")
    public ResponseEntity<Void> deletePostcard(@PathVariable Long id) {
        try {
            postcardService.deletePostcard(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting postcard: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/images/{username}/{subdir}/{filename:.+}")
    public ResponseEntity<Resource> serveUserImageWithSubdir(
            @PathVariable String username,
            @PathVariable String subdir,
            @PathVariable String filename) {
        return serveImageInternal(username + "/" + subdir + "/" + filename);
    }
    
    @GetMapping("/images/{username}/{filename:.+}")
    public ResponseEntity<Resource> serveUserImage(
            @PathVariable String username,
            @PathVariable String filename) {
        return serveImageInternal(username + "/" + filename);
    }
    
    @GetMapping("/images/{filename:.+}")
    public ResponseEntity<Resource> serveImage(@PathVariable String filename) {
        // Handle both old format (filename only) and new format (username/filename)
        return serveImageInternal(filename);
    }
    
    private ResponseEntity<Resource> serveImageInternal(String filePath) {
        try {
            Path path = fileStorageService.getFilePath(filePath);
            Resource resource = new UrlResource(path.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + path.getFileName() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error serving image: {}", filePath, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/polish-text")
    public ResponseEntity<PolishTextResponse> polishText(@RequestBody PolishTextRequest request) {
        try {
            String polishedText = aiService.polishText(request.getText(), request.getTemplateType());
            return ResponseEntity.ok(PolishTextResponse.builder()
                    .polishedText(polishedText)
                    .build());
        } catch (Exception e) {
            log.error("Error polishing text", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest request) {
        try {
            OrderResponse response = orderService.createOrder(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        List<OrderResponse> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        try {
            OrderResponse response = orderService.getOrder(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Order not found: {}", id);
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/downloads")
    public ResponseEntity<DownloadResponse> recordDownload(@RequestBody DownloadRequest request) {
        try {
            DownloadResponse response = downloadService.recordDownload(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error recording download", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/downloads")
    public ResponseEntity<List<DownloadResponse>> getAllDownloads() {
        List<DownloadResponse> downloads = downloadService.getAllDownloads();
        return ResponseEntity.ok(downloads);
    }
}
