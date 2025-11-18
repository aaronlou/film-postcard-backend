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
    
    public String storeFile(MultipartFile file) throws IOException {
        return storeFile(file, null);
    }
    
    public String storeFile(MultipartFile file, String username) throws IOException {
        // Validate file type - only JPG/JPEG allowed
        validateImageType(file);
        
        // Determine upload path
        Path uploadPath;
        if (username != null && !username.isEmpty()) {
            // User-specific directory: uploads/{username}/
            uploadPath = Paths.get(uploadDir, username);
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
        
        // Return relative path including username directory if applicable
        String relativePath = username != null && !username.isEmpty() 
                ? username + "/" + filename 
                : filename;
        
        log.info("File stored successfully: {} for user: {}", relativePath, username != null ? username : "anonymous");
        return relativePath;
    }
    
    public void deleteFile(String filename) throws IOException {
        // filename might be "username/uuid.jpg" or just "uuid.jpg"
        Path filePath = Paths.get(uploadDir).resolve(filename);
        Files.deleteIfExists(filePath);
        log.info("File deleted: {}", filename);
    }
    
    public Path getFilePath(String filename) {
        // filename might be "username/uuid.jpg" or just "uuid.jpg"
        return Paths.get(uploadDir).resolve(filename);
    }
    
    private void validateImageType(MultipartFile file) {
        // Check file content (Magic Bytes) - most reliable
        try {
            byte[] bytes = new byte[3];
            file.getInputStream().read(bytes);
            
            // JPG files start with: FF D8 FF
            if (!(bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF)) {
                throw new RuntimeException("File content is not a valid JPG/JPEG image");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file content");
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
