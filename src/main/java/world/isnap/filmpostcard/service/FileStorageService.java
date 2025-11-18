package world.isnap.filmpostcard.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {
    
    @Value("${file.upload-dir:uploads}")
    private String uploadDir;
    
    public enum FileType {
        AVATAR,   // uploads/{username}/avatar/
        PHOTO,    // uploads/{username}/photos/
        POSTCARD, // uploads/{username}/postcards/
        OTHER     // uploads/{username}/
    }
    
    /**
     * Store file result containing both path and size
     */
    @lombok.Data
    @lombok.Builder
    public static class StoredFile {
        private String relativePath;
        private Long fileSize;
    }
    
    public String storeFile(MultipartFile file) throws IOException {
        return storeFileWithSize(file, null, FileType.OTHER).getRelativePath();
    }
    
    public String storeFile(MultipartFile file, String username) throws IOException {
        return storeFileWithSize(file, username, FileType.OTHER).getRelativePath();
    }
    
    public String storeFile(MultipartFile file, String username, FileType fileType) throws IOException {
        return storeFileWithSize(file, username, fileType).getRelativePath();
    }
    
    public StoredFile storeFileWithSize(MultipartFile file, String username, FileType fileType) throws IOException {
        // Validate file type - only JPG/JPEG allowed
        validateImageType(file);
        
        // Determine upload path with subdirectory
        Path uploadPath;
        if (username != null && !username.isEmpty()) {
            // User-specific directory with file type subdirectory
            String subDir = getSubDirectory(fileType);
            uploadPath = Paths.get(uploadDir, username, subDir);
        } else {
            // Common directory: uploads/
            uploadPath = Paths.get(uploadDir);
        }
        
        // Create upload directory if not exists
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Generate unique filename with .jpg extension
        String originalFilename = file.getOriginalFilename();
        String fileExtension = ".jpg"; // Force .jpg extension
        if (originalFilename != null && originalFilename.toLowerCase().endsWith(".jpeg")) {
            fileExtension = ".jpeg"; // Keep .jpeg if that was the original
        }
        String filename = UUID.randomUUID().toString() + fileExtension;
        
        // Store file
        Path targetPath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        
        // Return relative path: username/subdir/filename
        String relativePath;
        if (username != null && !username.isEmpty()) {
            String subDir = getSubDirectory(fileType);
            if (subDir.isEmpty()) {
                relativePath = username + "/" + filename;
            } else {
                relativePath = username + "/" + subDir + "/" + filename;
            }
        } else {
            relativePath = filename;
        }
        
        log.info("File stored successfully: {} (type: {}) for user: {}", 
                relativePath, fileType, username != null ? username : "anonymous");
        
        return StoredFile.builder()
                .relativePath(relativePath)
                .fileSize(file.getSize())
                .build();
    }
    
    /**
     * Get subdirectory name based on file type
     */
    private String getSubDirectory(FileType fileType) {
        switch (fileType) {
            case AVATAR:
                return "avatar";
            case PHOTO:
                return "photos";
            case POSTCARD:
                return "postcards";
            case OTHER:
            default:
                return "";
        }
    }
    
    public void deleteFile(String filename) throws IOException {
        if (filename == null || filename.isEmpty()) {
            log.warn("Attempted to delete file with null/empty filename");
            return;
        }
        // filename might be "username/subdir/uuid.jpg" or "username/uuid.jpg" or just "uuid.jpg"
        Path filePath = Paths.get(uploadDir).resolve(filename);
        Files.deleteIfExists(filePath);
        log.info("File deleted: {}", filename);
    }
    
    public long getFileSize(String filename) throws IOException {
        if (filename == null || filename.isEmpty()) {
            return 0L;
        }
        // Get file size before deletion (for storage quota update)
        Path filePath = Paths.get(uploadDir).resolve(filename);
        if (Files.exists(filePath)) {
            return Files.size(filePath);
        }
        return 0L;
    }
    
    public Path getFilePath(String filename) {
        if (filename == null || filename.isEmpty()) {
            return Paths.get(uploadDir);
        }
        // filename might be "username/uuid.jpg" or just "uuid.jpg"
        return Paths.get(uploadDir).resolve(filename);
    }
    
    private void validateImageType(MultipartFile file) {
        // Check file content (Magic Bytes) - most reliable
        // IMPORTANT: Use try-with-resources to auto-close the stream
        // This ensures the next call to file.getInputStream() returns a fresh stream from the beginning
        try (var is = file.getInputStream()) {
            byte[] bytes = new byte[3];
            int bytesRead = is.read(bytes);
            
            if (bytesRead != 3) {
                throw new RuntimeException("File is too small to be a valid image");
            }
            
            // JPG files start with: FF D8 FF
            if (!(bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF)) {
                throw new RuntimeException("File content is not a valid JPG/JPEG image");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file content", e);
        }
        
        // Check content type (secondary validation)
        String contentType = file.getContentType();
        if (contentType == null || 
            (!contentType.equals("image/jpeg") && !contentType.equals("image/jpg"))) {
            throw new RuntimeException("Only JPG/JPEG images are allowed");
        }
        
        // Check file extension (tertiary validation)
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String lowerFilename = originalFilename.toLowerCase();
            if (!lowerFilename.endsWith(".jpg") && !lowerFilename.endsWith(".jpeg")) {
                throw new RuntimeException("File must have .jpg or .jpeg extension");
            }
        }
    }
}
