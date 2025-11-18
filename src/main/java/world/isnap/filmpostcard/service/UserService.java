package world.isnap.filmpostcard.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import world.isnap.filmpostcard.dto.UpdateUserProfileRequest;
import world.isnap.filmpostcard.dto.UserLoginRequest;
import world.isnap.filmpostcard.dto.UserLoginResponse;
import world.isnap.filmpostcard.dto.UserProfileResponse;
import world.isnap.filmpostcard.dto.UserRegistrationRequest;
import world.isnap.filmpostcard.entity.User;
import world.isnap.filmpostcard.repository.PhotoRepository;
import world.isnap.filmpostcard.repository.PostcardRepository;
import world.isnap.filmpostcard.repository.UserRepository;
import world.isnap.filmpostcard.util.JwtUtil;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final PostcardRepository postcardRepository;
    private final PhotoRepository photoRepository;
    private final FileStorageService fileStorageService;
    private final JwtUtil jwtUtil;
    
    @Transactional
    public UserProfileResponse registerUser(UserRegistrationRequest request) {
        // Validate username and email
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        
        // Hash password (simple implementation - in production use BCrypt)
        String passwordHash = hashPassword(request.getPassword());
        
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordHash)
                .displayName(request.getDisplayName() != null ? request.getDisplayName() : request.getUsername())
                .build();
        
        User saved = userRepository.save(user);
        log.info("User registered: {}", saved.getUsername());
        
        return toProfileResponse(saved);
    }
    
    public UserProfileResponse getUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return toProfileResponse(user);
    }
    
    public UserLoginResponse loginUser(UserLoginRequest request) {
        // Find user by username
        User user = userRepository.findByUsername(request.getUsername())
                .orElse(null);
        
        if (user == null) {
            return UserLoginResponse.builder()
                    .success(false)
                    .message("Invalid username or password")
                    .build();
        }
        
        // Check if account is active
        if (!user.getIsActive()) {
            return UserLoginResponse.builder()
                    .success(false)
                    .message("Account is disabled")
                    .build();
        }
        
        // Verify password
        String passwordHash = hashPassword(request.getPassword());
        if (!passwordHash.equals(user.getPasswordHash())) {
            return UserLoginResponse.builder()
                    .success(false)
                    .message("Invalid username or password")
                    .build();
        }
        
        log.info("User logged in: {}", user.getUsername());
        
        // Generate JWT token
        String token = jwtUtil.generateToken(user.getUsername(), user.getId());
        
        return UserLoginResponse.builder()
                .success(true)
                .message("Login successful")
                .token(token)
                .user(toProfileResponse(user))
                .build();
    }
    
    @Transactional
    public UserProfileResponse updateUserProfile(String username, UpdateUserProfileRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        
        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getWebsite() != null) {
            user.setWebsite(request.getWebsite());
        }
        if (request.getXiaohongshu() != null) {
            user.setXiaohongshu(request.getXiaohongshu());
        }
        if (request.getLocation() != null) {
            user.setLocation(request.getLocation());
        }
        
        User updated = userRepository.save(user);
        log.info("User profile updated: {}", username);
        
        return toProfileResponse(updated);
    }
    
    @Transactional
    public UserProfileResponse updateAvatar(String username, MultipartFile avatar) throws IOException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        
        // Delete old avatar if exists
        if (user.getAvatarUrl() != null) {
            try {
                String oldFilename = user.getAvatarUrl().replace("/api/images/", "");
                fileStorageService.deleteFile(oldFilename);
            } catch (Exception e) {
                log.warn("Failed to delete old avatar", e);
            }
        }
        
        // Store new avatar in user-specific directory
        String filename = fileStorageService.storeFile(avatar, username, FileStorageService.FileType.AVATAR);
        user.setAvatarUrl("/api/images/" + filename);
        
        User updated = userRepository.save(user);
        log.info("Avatar updated for user: {}", username);
        
        return toProfileResponse(updated);
    }
    
    public User getUserEntity(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }
    
    private UserProfileResponse toProfileResponse(User user) {
        Long designCount = postcardRepository.countByUser(user);
        Long photoCount = photoRepository.countByUser(user);
        
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .bio(user.getBio())
                .avatarUrl(user.getAvatarUrl())
                .website(user.getWebsite())
                .xiaohongshu(user.getXiaohongshu())
                .location(user.getLocation())
                .designCount(designCount)
                .photoCount(photoCount)
                .createdAt(user.getCreatedAt())
                .build();
    }
    
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash password", e);
        }
    }
}
