package world.isnap.filmpostcard.service;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@Slf4j
public class ImageResizeService {
    
    // Image size configurations
    private static final int THUMB_WIDTH = 300;      // Thumbnail width
    private static final int MEDIUM_WIDTH = 1280;    // Medium preview width
    private static final float QUALITY = 0.85f;       // JPEG quality (0.0 - 1.0)
    
    /**
     * Generate multiple versions of an image
     * @param originalPath Path to the original image file
     * @return ImageVersions containing paths to all generated versions
     */
    public ImageVersions generateImageVersions(Path originalPath) throws IOException {
        if (!Files.exists(originalPath)) {
            throw new IOException("Original image file does not exist: " + originalPath);
        }
        
        log.info("Generating image versions for: {}", originalPath);
        
        // Generate file names for different versions
        String originalFileName = originalPath.getFileName().toString();
        String baseName = originalFileName.substring(0, originalFileName.lastIndexOf('.'));
        String extension = originalFileName.substring(originalFileName.lastIndexOf('.'));
        
        Path parentDir = originalPath.getParent();
        Path thumbPath = parentDir.resolve(baseName + "_thumb" + extension);
        Path mediumPath = parentDir.resolve(baseName + "_medium" + extension);
        
        try {
            // Generate thumbnail (300px width)
            Thumbnails.of(originalPath.toFile())
                    .width(THUMB_WIDTH)
                    .outputQuality(QUALITY)
                    .toFile(thumbPath.toFile());
            log.info("Generated thumbnail: {} (size: {} bytes)", thumbPath, Files.size(thumbPath));
            
            // Generate medium preview (1280px width)
            Thumbnails.of(originalPath.toFile())
                    .width(MEDIUM_WIDTH)
                    .outputQuality(QUALITY)
                    .toFile(mediumPath.toFile());
            log.info("Generated medium: {} (size: {} bytes)", mediumPath, Files.size(mediumPath));
            
            return ImageVersions.builder()
                    .originalPath(originalPath)
                    .thumbPath(thumbPath)
                    .mediumPath(mediumPath)
                    .build();
            
        } catch (IOException e) {
            log.error("Failed to generate image versions for: {}", originalPath, e);
            // Clean up any partially created files
            deleteIfExists(thumbPath);
            deleteIfExists(mediumPath);
            throw new IOException("Failed to generate image versions: " + e.getMessage(), e);
        }
    }
    
    /**
     * Delete all versions of an image
     */
    public void deleteImageVersions(Path originalPath) {
        if (originalPath == null) {
            return;
        }
        
        String originalFileName = originalPath.getFileName().toString();
        String baseName = originalFileName.substring(0, originalFileName.lastIndexOf('.'));
        String extension = originalFileName.substring(originalFileName.lastIndexOf('.'));
        
        Path parentDir = originalPath.getParent();
        Path thumbPath = parentDir.resolve(baseName + "_thumb" + extension);
        Path mediumPath = parentDir.resolve(baseName + "_medium" + extension);
        
        deleteIfExists(originalPath);
        deleteIfExists(thumbPath);
        deleteIfExists(mediumPath);
        
        log.info("Deleted image versions for: {}", originalPath);
    }
    
    private void deleteIfExists(Path path) {
        try {
            if (path != null && Files.exists(path)) {
                Files.delete(path);
            }
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", path, e);
        }
    }
    
    @lombok.Data
    @lombok.Builder
    public static class ImageVersions {
        private Path originalPath;
        private Path thumbPath;
        private Path mediumPath;
        
        public String getOriginalRelativePath(Path baseDir) {
            return baseDir.relativize(originalPath).toString().replace("\\", "/");
        }
        
        public String getThumbRelativePath(Path baseDir) {
            return baseDir.relativize(thumbPath).toString().replace("\\", "/");
        }
        
        public String getMediumRelativePath(Path baseDir) {
            return baseDir.relativize(mediumPath).toString().replace("\\", "/");
        }
    }
}
