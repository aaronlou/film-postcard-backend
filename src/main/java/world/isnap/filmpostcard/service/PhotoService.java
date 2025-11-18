package world.isnap.filmpostcard.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import world.isnap.filmpostcard.dto.PhotoListResponse;
import world.isnap.filmpostcard.dto.PhotoResponse;
import world.isnap.filmpostcard.dto.PhotoUploadRequest;
import world.isnap.filmpostcard.entity.Photo;
import world.isnap.filmpostcard.entity.User;
import world.isnap.filmpostcard.repository.PhotoRepository;
import world.isnap.filmpostcard.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PhotoService {
    
    private final PhotoRepository photoRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final StorageQuotaService storageQuotaService;
    
    private static final int MAX_PHOTOS_PER_USER = 50;
    
    @Transactional
    public PhotoResponse uploadPhoto(String username, PhotoUploadRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        
        // Check photo limit
        Long photoCount = photoRepository.countByUser(user);
        if (photoCount >= MAX_PHOTOS_PER_USER) {
            throw new RuntimeException("Photo limit reached. Maximum " + MAX_PHOTOS_PER_USER + " photos per user.");
        }
        
        // Parse takenAt if provided
        LocalDateTime takenAt = null;
        if (request.getTakenAt() != null && !request.getTakenAt().isEmpty()) {
            try {
                takenAt = LocalDateTime.parse(request.getTakenAt(), DateTimeFormatter.ISO_DATE_TIME);
            } catch (Exception e) {
                log.warn("Failed to parse takenAt date: {}", request.getTakenAt());
            }
        }
        
        Photo photo = Photo.builder()
                .user(user)
                .imageUrl(request.getImageUrl())
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .camera(request.getCamera())
                .lens(request.getLens())
                .settings(request.getSettings())
                .takenAt(takenAt)
                .build();
        
        Photo savedPhoto = photoRepository.save(photo);
        log.info("Photo uploaded: {} by user: {}", savedPhoto.getId(), username);
        
        return toPhotoResponse(savedPhoto);
    }
    
    @Transactional(readOnly = true)
    public PhotoListResponse getUserPhotos(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        
        List<Photo> photos = photoRepository.findByUserOrderByCreatedAtDesc(user);
        List<PhotoResponse> photoResponses = photos.stream()
                .map(this::toPhotoResponse)
                .collect(Collectors.toList());
        
        return PhotoListResponse.builder()
                .photos(photoResponses)
                .total(photoResponses.size())
                .build();
    }
    
    @Transactional
    public void deletePhoto(String username, String photoId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        
        Long photoIdLong;
        try {
            photoIdLong = Long.parseLong(photoId);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid photo ID: " + photoId);
        }
        
        Photo photo = photoRepository.findById(photoIdLong)
                .orElseThrow(() -> new RuntimeException("Photo not found: " + photoId));
        
        // Verify ownership
        if (!photo.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You can only delete your own photos");
        }
        
        // Extract filename from imageUrl: /api/images/username/photos/uuid.jpg -> username/photos/uuid.jpg
        String imageUrl = photo.getImageUrl();
        String filename = imageUrl.replace("/api/images/", "");
        
        try {
            // Get file size before deletion (for storage quota update)
            long fileSize = fileStorageService.getFileSize(filename);
            
            // Delete physical file
            fileStorageService.deleteFile(filename);
            
            // Delete database record
            photoRepository.delete(photo);
            
            // Update user's storage usage
            if (fileSize > 0) {
                storageQuotaService.decrementStorage(user, fileSize);
            }
            
            log.info("Photo deleted: {} by user: {}, freed {} bytes", photoId, username, fileSize);
        } catch (Exception e) {
            log.error("Failed to delete photo file: {}", filename, e);
            throw new RuntimeException("Failed to delete photo: " + e.getMessage());
        }
    }
    
    private PhotoResponse toPhotoResponse(Photo photo) {
        return PhotoResponse.builder()
                .id(String.valueOf(photo.getId()))
                .imageUrl(photo.getImageUrl())
                .title(photo.getTitle())
                .description(photo.getDescription())
                .location(photo.getLocation())
                .camera(photo.getCamera())
                .lens(photo.getLens())
                .settings(photo.getSettings())
                .takenAt(photo.getTakenAt() != null ? photo.getTakenAt().toString() : null)
                .createdAt(photo.getCreatedAt().toString())
                .build();
    }
}
