package world.isnap.filmpostcard.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import world.isnap.filmpostcard.entity.User;
import world.isnap.filmpostcard.entity.UserTier;
import world.isnap.filmpostcard.repository.PhotoRepository;
import world.isnap.filmpostcard.repository.UserRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class StorageQuotaService {
    
    private final UserRepository userRepository;
    private final PhotoRepository photoRepository;
    
    /**
     * Check if user can upload a file of given size
     */
    public void validateUpload(User user, long fileSize) {
        UserTier tier = UserTier.fromString(user.getUserTier());
        
        // Check single file size limit
        if (fileSize > tier.getSingleFileLimit()) {
            throw new RuntimeException(
                String.format("File size %s exceeds your %s tier limit of %s per file. Please upgrade your account.",
                    formatBytes(fileSize),
                    tier.getDisplayName(),
                    tier.getSingleFileLimitFormatted())
            );
        }
        
        // Check total storage quota
        long storageUsed = user.getStorageUsed() != null ? user.getStorageUsed() : 0L;
        if (storageUsed + fileSize > tier.getStorageLimit()) {
            long available = tier.getStorageLimit() - storageUsed;
            throw new RuntimeException(
                String.format("Insufficient storage. You have %s available out of %s total (%s tier). File requires %s.",
                    formatBytes(available),
                    tier.getStorageLimitFormatted(),
                    tier.getDisplayName(),
                    formatBytes(fileSize))
            );
        }
        
        // Check photo count limit
        Long photoCount = photoRepository.countByUser(user);
        if (photoCount >= tier.getPhotoLimit()) {
            throw new RuntimeException(
                String.format("Photo limit reached. Your %s tier allows %d photos. Please delete some photos or upgrade your account.",
                    tier.getDisplayName(),
                    tier.getPhotoLimit())
            );
        }
    }
    
    /**
     * Update user's storage usage after successful upload
     */
    @Transactional
    public void incrementStorage(User user, long fileSize) {
        long currentUsage = user.getStorageUsed() != null ? user.getStorageUsed() : 0L;
        user.setStorageUsed(currentUsage + fileSize);
        userRepository.save(user);
        log.info("Storage updated for user {}: {} bytes (+{} bytes)", 
                user.getUsername(), user.getStorageUsed(), fileSize);
    }
    
    /**
     * Update user's storage usage after file deletion
     */
    @Transactional
    public void decrementStorage(User user, long fileSize) {
        long currentUsage = user.getStorageUsed() != null ? user.getStorageUsed() : 0L;
        user.setStorageUsed(Math.max(0, currentUsage - fileSize));
        userRepository.save(user);
        log.info("Storage updated for user {}: {} bytes (-{} bytes)", 
                user.getUsername(), user.getStorageUsed(), fileSize);
    }
    
    /**
     * Get user's storage quota information
     */
    public StorageQuotaInfo getQuotaInfo(User user) {
        UserTier tier = UserTier.fromString(user.getUserTier());
        long storageUsed = user.getStorageUsed() != null ? user.getStorageUsed() : 0L;
        Long photoCount = photoRepository.countByUser(user);
        
        return StorageQuotaInfo.builder()
                .tier(tier.name())
                .tierDisplayName(tier.getDisplayName())
                .storageUsed(storageUsed)
                .storageLimit(tier.getStorageLimit())
                .storageUsedFormatted(formatBytes(storageUsed))
                .storageLimitFormatted(tier.getStorageLimitFormatted())
                .storagePercentage((int) ((storageUsed * 100) / tier.getStorageLimit()))
                .photoCount(photoCount.intValue())
                .photoLimit(tier.getPhotoLimit())
                .singleFileLimit(tier.getSingleFileLimit())
                .singleFileLimitFormatted(tier.getSingleFileLimitFormatted())
                .build();
    }
    
    private String formatBytes(long bytes) {
        if (bytes >= 1024 * 1024 * 1024) {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        } else if (bytes >= 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else if (bytes >= 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else {
            return bytes + " bytes";
        }
    }
    
    @lombok.Data
    @lombok.Builder
    public static class StorageQuotaInfo {
        private String tier;
        private String tierDisplayName;
        private Long storageUsed;
        private Long storageLimit;
        private String storageUsedFormatted;
        private String storageLimitFormatted;
        private Integer storagePercentage;
        private Integer photoCount;
        private Integer photoLimit;
        private Long singleFileLimit;
        private String singleFileLimitFormatted;
    }
}
