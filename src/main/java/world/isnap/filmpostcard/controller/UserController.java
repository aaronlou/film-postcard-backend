package world.isnap.filmpostcard.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import world.isnap.filmpostcard.dto.*;
import world.isnap.filmpostcard.service.PhotoService;
import world.isnap.filmpostcard.service.PostcardService;
import world.isnap.filmpostcard.service.StorageQuotaService;
import world.isnap.filmpostcard.service.UserService;
import world.isnap.filmpostcard.util.JwtUtil;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class UserController {
    
    private final UserService userService;
    private final PostcardService postcardService;
    private final PhotoService photoService;
    private final JwtUtil jwtUtil;
    private final StorageQuotaService storageQuotaService;
    
    @PostMapping("/register")
    public ResponseEntity<UserProfileResponse> register(@RequestBody UserRegistrationRequest request) {
        try {
            UserProfileResponse response = userService.registerUser(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Registration error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<UserLoginResponse> login(@RequestBody UserLoginRequest request) {
        try {
            UserLoginResponse response = userService.loginUser(request);
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(401).body(response);
            }
        } catch (Exception e) {
            log.error("Login error: {}", e.getMessage());
            return ResponseEntity.status(500).body(
                UserLoginResponse.builder()
                    .success(false)
                    .message("Internal server error")
                    .build()
            );
        }
    }
    
    @GetMapping("/{username}")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable String username) {
        try {
            UserProfileResponse response = userService.getUserProfile(username);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("User not found: {}", username);
            return ResponseEntity.notFound().build();
        }
    }
    
    @PutMapping("/{username}")
    public ResponseEntity<?> updateProfile(
            @PathVariable String username,
            @RequestBody UpdateUserProfileRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Verify JWT and match username
            String authenticatedUser = extractAndVerifyUser(authHeader, username);
            if (authenticatedUser == null) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "Forbidden", "message", "You can only update your own profile"));
            }
            
            UserProfileResponse response = userService.updateUserProfile(username, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error updating profile: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Bad Request", "message", e.getMessage()));
        }
    }
    
    @PostMapping("/{username}/avatar")
    public ResponseEntity<?> uploadAvatar(
            @PathVariable String username,
            @RequestParam("avatar") MultipartFile avatar,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Verify JWT and match username
            String authenticatedUser = extractAndVerifyUser(authHeader, username);
            if (authenticatedUser == null) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "Forbidden", "message", "You can only upload your own avatar"));
            }
            
            UserProfileResponse response = userService.updateAvatar(username, avatar);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Error uploading avatar", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Internal Server Error", "message", "Failed to upload avatar"));
        } catch (RuntimeException e) {
            log.error("Error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Bad Request", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/{username}/designs")
    public ResponseEntity<List<PostcardResponse>> getUserDesigns(@PathVariable String username) {
        try {
            // Return user's postcards
            List<PostcardResponse> postcards = postcardService.getUserPostcards(username);
            return ResponseEntity.ok(postcards);
        } catch (RuntimeException e) {
            log.error("Error getting user designs: {}", e.getMessage());
            return ResponseEntity.ok(List.of()); // Return empty list instead of error
        }
    }
    
    @GetMapping("/{username}/photos")
    public ResponseEntity<PhotoListResponse> getUserPhotos(@PathVariable String username) {
        try {
            PhotoListResponse response = photoService.getUserPhotos(username);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error getting user photos: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/{username}/photos")
    public ResponseEntity<?> uploadPhoto(
            @PathVariable String username,
            @RequestBody PhotoUploadRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Verify JWT and match username
            String authenticatedUser = extractAndVerifyUser(authHeader, username);
            if (authenticatedUser == null) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "Forbidden", "message", "You can only upload photos to your own profile"));
            }
            
            PhotoResponse response = photoService.uploadPhoto(username, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error uploading photo: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Bad Request", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/{username}/quota")
    public ResponseEntity<?> getStorageQuota(
            @PathVariable String username,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Verify JWT and match username
            String authenticatedUser = extractAndVerifyUser(authHeader, username);
            if (authenticatedUser == null) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "Forbidden", "message", "You can only view your own quota"));
            }
            
            world.isnap.filmpostcard.entity.User user = userService.getUserEntity(username);
            StorageQuotaService.StorageQuotaInfo quotaInfo = storageQuotaService.getQuotaInfo(user);
            return ResponseEntity.ok(quotaInfo);
        } catch (RuntimeException e) {
            log.error("Error getting quota: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Bad Request", "message", e.getMessage()));
        }
    }
    
    @DeleteMapping("/{username}/photos/{photoId}")
    public ResponseEntity<?> deletePhoto(
            @PathVariable String username,
            @PathVariable String photoId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Verify JWT and match username
            String authenticatedUser = extractAndVerifyUser(authHeader, username);
            if (authenticatedUser == null) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "Forbidden", "message", "You can only delete your own photos"));
            }
            
            photoService.deletePhoto(username, photoId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Photo deleted successfully"));
        } catch (RuntimeException e) {
            log.error("Error deleting photo: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Bad Request", "message", e.getMessage()));
        }
    }
    
    /**
     * Extract username from JWT and verify it matches the path username
     * @param authHeader Authorization header (Bearer token)
     * @param pathUsername Username from URL path
     * @return authenticated username if valid and matches, null otherwise
     */
    private String extractAndVerifyUser(String authHeader, String pathUsername) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header");
            return null;
        }
        
        String token = authHeader.substring(7); // Remove "Bearer " prefix
        
        if (!jwtUtil.validateToken(token)) {
            log.warn("Invalid JWT token");
            return null;
        }
        
        if (jwtUtil.isTokenExpired(token)) {
            log.warn("Expired JWT token");
            return null;
        }
        
        String authenticatedUsername = jwtUtil.getUsernameFromToken(token);
        if (authenticatedUsername == null) {
            log.warn("Unable to extract username from token");
            return null;
        }
        
        // Verify the authenticated user matches the path username
        if (!authenticatedUsername.equals(pathUsername)) {
            log.warn("Authenticated user {} does not match path username {}", authenticatedUsername, pathUsername);
            return null;
        }
        
        log.debug("Authenticated and authorized user: {}", authenticatedUsername);
        return authenticatedUsername;
    }
}
