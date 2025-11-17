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
import world.isnap.filmpostcard.dto.*;
import world.isnap.filmpostcard.service.AIService;
import world.isnap.filmpostcard.service.FileStorageService;
import world.isnap.filmpostcard.service.OrderService;
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
    private final AIService aiService;
    private final OrderService orderService;
    
    @PostMapping("/postcards")
    public ResponseEntity<PostcardResponse> createPostcard(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "text", required = false) String textContent,
            @RequestParam(value = "templateType", required = false) String templateType,
            @RequestParam(value = "qrUrl", required = false) String qrUrl) {
        try {
            PostcardResponse response = postcardService.createPostcard(image, textContent, templateType, qrUrl);
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
    
    @PostMapping("/polish-text")
    public ResponseEntity<PolishTextResponse> polishText(@RequestBody PolishTextRequest request) {
        try {
            String polishedText = aiService.polishText(request.getText(), request.getTemplateType());
            return ResponseEntity.ok(PolishTextResponse.builder()
                    .polishedText(polishedText)
                    .build());
        } catch (Exception e) {
            log.error("Error polishing text", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest request) {
        try {
            OrderResponse response = orderService.createOrder(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        List<OrderResponse> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        try {
            OrderResponse response = orderService.getOrder(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Order not found: {}", id);
            return ResponseEntity.notFound().build();
        }
    }
}
