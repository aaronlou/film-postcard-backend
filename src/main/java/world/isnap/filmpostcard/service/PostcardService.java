package world.isnap.filmpostcard.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import world.isnap.filmpostcard.dto.PostcardResponse;
import world.isnap.filmpostcard.entity.Postcard;
import world.isnap.filmpostcard.entity.User;
import world.isnap.filmpostcard.repository.PostcardRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostcardService {
    
    private final PostcardRepository postcardRepository;
    private final FileStorageService fileStorageService;
    private final UserService userService;
    
    @Transactional
    public PostcardResponse createPostcardFromImage(String imageFilename, String textContent, String templateType, String qrUrl, String username) throws IOException {
        // Validate that the image file exists
        Path imagePath = fileStorageService.getFilePath(imageFilename);
        if (!Files.exists(imagePath)) {
            throw new RuntimeException("Image file not found: " + imageFilename);
        }
        
        long fileSize = Files.size(imagePath);
        
        // Get user if username provided
        User user = null;
        if (username != null && !username.isEmpty()) {
            user = userService.getUserEntity(username);
        }
        
        // Create postcard entity
        Postcard postcard = Postcard.builder()
                .user(user)
                .imagePath(imageFilename)
                .textContent(textContent)
                .originalFilename(imageFilename)
                .fileSize(fileSize)
                .templateType(templateType != null ? templateType : "postcard")
                .qrUrl(qrUrl)
                .build();
        
        Postcard saved = postcardRepository.save(postcard);
        log.info("Postcard created from existing image with ID: {}", saved.getId());
        
        return toResponse(saved);
    }
    
    @Transactional
    public PostcardResponse createPostcard(MultipartFile image, String textContent, String templateType, String qrUrl, String username) throws IOException {
        // Validate image
        validateImage(image);
        
        // Get user if username provided
        User user = null;
        if (username != null && !username.isEmpty()) {
            user = userService.getUserEntity(username);
        }
        
        // Store file in user-specific directory
        String filename = fileStorageService.storeFile(image, username);
        
        // Create postcard entity
        Postcard postcard = Postcard.builder()
                .user(user)
                .imagePath(filename)
                .textContent(textContent)
                .originalFilename(image.getOriginalFilename())
                .fileSize(image.getSize())
                .templateType(templateType != null ? templateType : "postcard")
                .qrUrl(qrUrl)
                .build();
        
        Postcard saved = postcardRepository.save(postcard);
        log.info("Postcard created with ID: {}", saved.getId());
        
        return toResponse(saved);
    }
    
    public PostcardResponse getPostcard(Long id) {
        Postcard postcard = postcardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Postcard not found with id: " + id));
        return toResponse(postcard);
    }
    
    public List<PostcardResponse> getAllPostcards() {
        return postcardRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    public List<PostcardResponse> getUserPostcards(String username) {
        User user = userService.getUserEntity(username);
        return postcardRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public PostcardResponse updatePostcard(Long id, String textContent, String templateType, String qrUrl) {
        Postcard postcard = postcardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Postcard not found with id: " + id));
        
        // Update fields if provided
        if (textContent != null) {
            postcard.setTextContent(textContent);
        }
        if (templateType != null) {
            postcard.setTemplateType(templateType);
        }
        if (qrUrl != null) {
            postcard.setQrUrl(qrUrl);
        }
        
        Postcard updated = postcardRepository.save(postcard);
        log.info("Postcard updated with ID: {}", updated.getId());
        
        return toResponse(updated);
    }
    
    @Transactional
    public void deletePostcard(Long id) throws IOException {
        Postcard postcard = postcardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Postcard not found with id: " + id));
        
        // Delete file
        fileStorageService.deleteFile(postcard.getImagePath());
        
        // Delete record
        postcardRepository.delete(postcard);
        log.info("Postcard deleted with ID: {}", id);
    }
    
    private void validateImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }
        
        // Check file size (max 30MB)
        long maxSize = 30 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new RuntimeException("File size exceeds maximum limit of 30MB");
        }
        
        // Check file content (Magic Bytes) - most reliable validation
        try {
            byte[] bytes = new byte[3];
            file.getInputStream().read(bytes);
            
            // JPG files start with: FF D8 FF
            if (!(bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF)) {
                throw new RuntimeException("File content is not a valid JPG/JPEG image");
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to read file content");
        }
        
        // Check content type - only JPG/JPEG allowed
        String contentType = file.getContentType();
        if (contentType == null || 
            (!contentType.equals("image/jpeg") && !contentType.equals("image/jpg"))) {
            throw new RuntimeException("Only JPG/JPEG images are allowed");
        }
        
        // Check file extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String lowerFilename = originalFilename.toLowerCase();
            if (!lowerFilename.endsWith(".jpg") && !lowerFilename.endsWith(".jpeg")) {
                throw new RuntimeException("File must have .jpg or .jpeg extension");
            }
        }
    }
    
    private PostcardResponse toResponse(Postcard postcard) {
        return PostcardResponse.builder()
                .id(postcard.getId())
                .imageUrl("/api/images/" + postcard.getImagePath())
                .textContent(postcard.getTextContent())
                .originalFilename(postcard.getOriginalFilename())
                .fileSize(postcard.getFileSize())
                .templateType(postcard.getTemplateType())
                .qrUrl(postcard.getQrUrl())
                .createdAt(postcard.getCreatedAt())
                .build();
    }
}
