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
import world.isnap.filmpostcard.service.PostcardService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

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
    
    @PostMapping("/upload")
    public ResponseEntity<ImageUploadResponse> uploadImage(@RequestParam("image") MultipartFile image) {
        try {
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
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new RuntimeException("Invalid file type. Only images are allowed");
            }
            
            // Only store file, don't create postcard yet
            String filename = fileStorageService.storeFile(image);
            String imageUrl = "/api/images/" + filename;
            
            ImageUploadResponse response = ImageUploadResponse.builder()
                    .id(filename)  // Return filename as temporary ID
                    .url(imageUrl)
                    .filename(filename)
                    .fileSize(image.getSize())
                    .build();
            
            log.info("Image uploaded successfully: {}", filename);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Error uploading image", e);
            return ResponseEntity.internalServerError().build();
        } catch (RuntimeException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/postcards")
    public ResponseEntity<PostcardResponse> createPostcard(
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "imageFilename", required = false) String imageFilename,
            @RequestParam(value = "text", required = false) String textContent,
            @RequestParam(value = "templateType", required = false) String templateType,
            @RequestParam(value = "qrUrl", required = false) String qrUrl) {
        try {
            PostcardResponse response;
            
            // Support two modes: direct upload OR use existing image
            if (image != null && !image.isEmpty()) {
                // Mode 1: Upload new image directly
                response = postcardService.createPostcard(image, textContent, templateType, qrUrl);
            } else if (imageFilename != null && !imageFilename.isEmpty()) {
                // Mode 2: Use already uploaded image
                response = postcardService.createPostcardFromImage(imageFilename, textContent, templateType, qrUrl);
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
    
    @GetMapping("/images/{filename:.+}")
    public ResponseEntity<Resource> serveImage(@PathVariable String filename) {
        try {
            Path filePath = fileStorageService.getFilePath(filename);
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error serving image: {}", filename, e);
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
