package world.isnap.filmpostcard.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import world.isnap.filmpostcard.dto.PostcardResponse;
import world.isnap.filmpostcard.entity.Postcard;
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
    
    @Transactional
    public PostcardResponse createPostcardFromImage(String imageFilename, String textContent, String templateType, String qrUrl) throws IOException {
        // Validate that the image file exists
        Path imagePath = fileStorageService.getFilePath(imageFilename);
        if (!Files.exists(imagePath)) {
            throw new RuntimeException("Image file not found: " + imageFilename);
        }
        
        long fileSize = Files.size(imagePath);
        
        // Create postcard entity
        Postcard postcard = Postcard.builder()
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
    public PostcardResponse createPostcard(MultipartFile image, String textContent, String templateType, String qrUrl) throws IOException {
        // Validate image
        validateImage(image);
        
        // Store file
        String filename = fileStorageService.storeFile(image);
        
        // Create postcard entity
        Postcard postcard = Postcard.builder()
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
        
        // Check file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("File must be an image");
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
