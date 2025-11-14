package world.isnap.filmpostcard.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import world.isnap.filmpostcard.dto.PostcardResponse;
import world.isnap.filmpostcard.service.FileStorageService;
import world.isnap.filmpostcard.service.PostcardService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PostcardController {
    
    private final PostcardService postcardService;
    private final FileStorageService fileStorageService;
    
    @PostMapping("/postcards")
    public ResponseEntity<PostcardResponse> createPostcard(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "text", required = false) String textContent) {
        try {
            PostcardResponse response = postcardService.createPostcard(image, textContent);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Error creating postcard", e);
            return ResponseEntity.internalServerError().build();
        } catch (RuntimeException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/postcards/{id}")
    public ResponseEntity<PostcardResponse> getPostcard(@PathVariable Long id) {
        try {
            PostcardResponse response = postcardService.getPostcard(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Postcard not found: {}", id);
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/postcards")
    public ResponseEntity<List<PostcardResponse>> getAllPostcards() {
        List<PostcardResponse> postcards = postcardService.getAllPostcards();
        return ResponseEntity.ok(postcards);
    }
    
    @DeleteMapping("/postcards/{id}")
    public ResponseEntity<Void> deletePostcard(@PathVariable Long id) {
        try {
            postcardService.deletePostcard(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting postcard: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/images/{filename:.+}")
    public ResponseEntity<Resource> serveImage(@PathVariable String filename) {
        try {
            Path filePath = fileStorageService.getFilePath(filename);
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error serving image: {}", filename, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
