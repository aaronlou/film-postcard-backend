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
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostcardService {
    
    private final PostcardRepository postcardRepository;
    private final FileStorageService fileStorageService;
    
    @Transactional
    public PostcardResponse createPostcard(MultipartFile image, String textContent) throws IOException {
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
        
        // Check file size (max 10MB)
        long maxSize = 10 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new RuntimeException("File size exceeds maximum limit of 10MB");
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
                .createdAt(postcard.getCreatedAt())
                .build();
    }
}
