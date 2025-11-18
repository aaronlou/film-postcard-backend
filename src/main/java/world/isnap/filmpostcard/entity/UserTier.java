package world.isnap.filmpostcard.entity;

import lombok.Getter;

@Getter
public enum UserTier {
    FREE("Free", 50 * 1024 * 1024L, 20, 10 * 1024 * 1024L),           // 50MB, 20 photos, 10MB per photo
    BASIC("Basic", 200 * 1024 * 1024L, 50, 20 * 1024 * 1024L),       // 200MB, 50 photos, 20MB per photo
    PRO("Pro", 2L * 1024 * 1024 * 1024, 100, 30 * 1024 * 1024L);    // 2GB, 100 photos, 30MB per photo

    private final String displayName;
    private final Long storageLimit;      // Total storage quota in bytes
    private final Integer photoLimit;     // Maximum number of photos
    private final Long singleFileLimit;   // Maximum size per file in bytes

    UserTier(String displayName, Long storageLimit, Integer photoLimit, Long singleFileLimit) {
        this.displayName = displayName;
        this.storageLimit = storageLimit;
        this.photoLimit = photoLimit;
        this.singleFileLimit = singleFileLimit;
    }

    public String getStorageLimitFormatted() {
        if (storageLimit >= 1024 * 1024 * 1024) {
            return (storageLimit / (1024 * 1024 * 1024)) + "GB";
        } else {
            return (storageLimit / (1024 * 1024)) + "MB";
        }
    }

    public String getSingleFileLimitFormatted() {
        if (singleFileLimit >= 1024 * 1024) {
            return (singleFileLimit / (1024 * 1024)) + "MB";
        } else {
            return (singleFileLimit / 1024) + "KB";
        }
    }

    public static UserTier fromString(String tierName) {
        if (tierName == null) {
            return FREE;
        }
        try {
            return UserTier.valueOf(tierName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return FREE;
        }
    }
}
