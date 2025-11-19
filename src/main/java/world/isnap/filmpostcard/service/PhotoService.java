package world.isnap.filmpostcard.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import world.isnap.filmpostcard.dto.PhotoListResponse;
import world.isnap.filmpostcard.dto.PhotoResponse;
import world.isnap.filmpostcard.dto.PhotoUploadRequest;
import world.isnap.filmpostcard.dto.UpdatePhotoRequest;
import world.isnap.filmpostcard.entity.Album;
import world.isnap.filmpostcard.entity.Photo;
import world.isnap.filmpostcard.entity.User;
import world.isnap.filmpostcard.repository.AlbumRepository;
import world.isnap.filmpostcard.repository.PhotoRepository;
import world.isnap.filmpostcard.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PhotoService {
    
    private final PhotoRepository photoRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final StorageQuotaService storageQuotaService;
    private final AlbumRepository albumRepository;
    
    private static final int MAX_PHOTOS_PER_USER = 50;
    
    @Transactional
    public PhotoResponse uploadPhoto(String username, PhotoUploadRequest request) {
        log.info("uploadPhoto called - username: {}, imageUrl: {}", username, request.getImageUrl());
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        
        // Idempotency check: if photo with same imageUrl already exists, update it instead of creating new
        Optional<Photo> existingPhoto = photoRepository.findByUserAndImageUrl(user, request.getImageUrl());
        if (existingPhoto.isPresent()) {
            log.info("Photo with imageUrl {} already exists for user {}, updating with new metadata (idempotent behavior)", 
                    request.getImageUrl(), username);
            return updateExistingPhoto(existingPhoto.get(), request);
        }
        
        // Check photo limit only for new photos
        Long photoCount = photoRepository.countByUser(user);
        if (photoCount >= MAX_PHOTOS_PER_USER) {
            throw new RuntimeException("Photo limit reached. Maximum " + MAX_PHOTOS_PER_USER + " photos per user.");
        }
        
        // Create new photo
        return createNewPhoto(user, request);
    }
    
    private PhotoResponse createNewPhoto(User user, PhotoUploadRequest request) {
        log.info("Creating new photo with metadata - title: {}, location: {}, camera: {}, albumId: {}",
                request.getTitle(), request.getLocation(), request.getCamera(), request.getAlbumId());
        
        // Parse takenAt if provided
        LocalDateTime takenAt = null;
        if (request.getTakenAt() != null && !request.getTakenAt().isEmpty()) {
            try {
                // Try ISO_DATE_TIME format first (e.g., "2024-11-19T10:30:00")
                takenAt = LocalDateTime.parse(request.getTakenAt(), DateTimeFormatter.ISO_DATE_TIME);
            } catch (Exception e1) {
                try {
                    // Try ISO_DATE format (e.g., "2024-11-19") and set time to midnight
                    takenAt = LocalDateTime.parse(request.getTakenAt() + "T00:00:00", DateTimeFormatter.ISO_DATE_TIME);
                } catch (Exception e2) {
                    log.warn("Failed to parse takenAt date: {}", request.getTakenAt());
                }
            }
        }
        
        // Parse albumId if provided
        Album album = null;
        if (request.getAlbumId() != null && !request.getAlbumId().isEmpty()) {
            log.info("Attempting to assign photo to album ID: {} ", request.getAlbumId());
            try {
                Long albumIdLong = Long.parseLong(request.getAlbumId());
                album = albumRepository.findByIdAndUser(albumIdLong, user)
                        .orElseThrow(() -> new RuntimeException("Album not found or not owned by user: " + request.getAlbumId()));
                log.info("Assigning photo to album: {} ({})", album.getName(), albumIdLong);
            } catch (NumberFormatException e) {
                log.error("Invalid album ID format: {} for user: {}", request.getAlbumId(), user, e);
                throw new RuntimeException("Invalid album ID format: " + request.getAlbumId());
            } catch (RuntimeException e) {
                log.error("Album not found or not owned by user: {} for user: {}", request.getAlbumId(), e);
                throw new RuntimeException("Album not found or not owned by user: " + request.getAlbumId());
            }
        } else {
            log.info("No album ID provided for photo upload by user: {}");
        }
        
        Photo photo = Photo.builder()
                .user(user)
                .album(album)
                .imageUrl(request.getImageUrl())
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .camera(request.getCamera())
                .lens(request.getLens())
                .settings(request.getSettings())
                .takenAt(takenAt)
                .build();
        
        log.info("Creating photo with metadata - title: {}, description: {}, location: {}, camera: {}, lens: {}, settings: {}, takenAt: {}, album: {}",
                photo.getTitle(), photo.getDescription(), photo.getLocation(), 
                photo.getCamera(), photo.getLens(), photo.getSettings(), 
                photo.getTakenAt(), album != null ? album.getName() : "none");
        
        Photo savedPhoto = photoRepository.save(photo);
        log.info("New photo created: {} by user: {}", savedPhoto.getId(), user.getUsername());
        
        return toPhotoResponse(savedPhoto);
    }
    
    private PhotoResponse updateExistingPhoto(Photo photo, PhotoUploadRequest request) {
        log.info("Updating existing photo {} with new metadata", photo.getId());
        
        // Update all metadata fields
        photo.setTitle(request.getTitle());
        photo.setDescription(request.getDescription());
        photo.setLocation(request.getLocation());
        photo.setCamera(request.getCamera());
        photo.setLens(request.getLens());
        photo.setSettings(request.getSettings());
        
        // Parse and update takenAt
        LocalDateTime takenAt = null;
        if (request.getTakenAt() != null && !request.getTakenAt().isEmpty()) {
            try {
                takenAt = LocalDateTime.parse(request.getTakenAt(), DateTimeFormatter.ISO_DATE_TIME);
            } catch (Exception e1) {
                try {
                    takenAt = LocalDateTime.parse(request.getTakenAt() + "T00:00:00", DateTimeFormatter.ISO_DATE_TIME);
                } catch (Exception e2) {
                    log.warn("Failed to parse takenAt date: {}", request.getTakenAt());
                }
            }
        }
        photo.setTakenAt(takenAt);
        
        // Update album
        Album album = null;
        if (request.getAlbumId() != null && !request.getAlbumId().isEmpty()) {
            try {
                Long albumIdLong = Long.parseLong(request.getAlbumId());
                album = albumRepository.findByIdAndUser(albumIdLong, photo.getUser())
                        .orElseThrow(() -> new RuntimeException("Album not found or not owned by user: " + request.getAlbumId()));
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid album ID format: " + request.getAlbumId());
            }
        }
        photo.setAlbum(album);
        
        Photo updatedPhoto = photoRepository.save(photo);
        log.info("Photo {} updated successfully", updatedPhoto.getId());
        
        return toPhotoResponse(updatedPhoto);
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
    
    @Transactional
    public PhotoResponse updatePhoto(String username, String photoId, UpdatePhotoRequest request) {
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
            throw new RuntimeException("You can only update your own photos");
        }
        
        // Update album association if provided
        if (request.getAlbumId() != null) {
            if (request.getAlbumId().isEmpty()) {
                // Remove from album
                photo.setAlbum(null);
            } else {
                // Move to specified album
                Long albumIdLong;
                try {
                    albumIdLong = Long.parseLong(request.getAlbumId());
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Invalid album ID: " + request.getAlbumId());
                }
                
                Album album = albumRepository.findByIdAndUser(albumIdLong, user)
                        .orElseThrow(() -> new RuntimeException("Album not found or not owned by user"));
                photo.setAlbum(album);
            }
        }
        
        // Update other metadata if provided
        if (request.getTitle() != null) {
            photo.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            photo.setDescription(request.getDescription());
        }
        if (request.getLocation() != null) {
            photo.setLocation(request.getLocation());
        }
        if (request.getCamera() != null) {
            photo.setCamera(request.getCamera());
        }
        if (request.getLens() != null) {
            photo.setLens(request.getLens());
        }
        if (request.getSettings() != null) {
            photo.setSettings(request.getSettings());
        }
        if (request.getTakenAt() != null && !request.getTakenAt().isEmpty()) {
            try {
                photo.setTakenAt(LocalDateTime.parse(request.getTakenAt(), DateTimeFormatter.ISO_DATE_TIME));
            } catch (Exception e) {
                log.warn("Failed to parse takenAt date: {}", request.getTakenAt());
            }
        }
        
        Photo updatedPhoto = photoRepository.save(photo);
        log.info("Photo updated: {} by user: {}", photoId, username);
        
        return toPhotoResponse(updatedPhoto);
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
                .albumId(photo.getAlbum() != null ? String.valueOf(photo.getAlbum().getId()) : null)
                .build();
    }
}
