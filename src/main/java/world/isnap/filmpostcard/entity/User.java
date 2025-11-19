package world.isnap.filmpostcard.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "username", unique = true, nullable = false)
    private String username;
    
    @Column(name = "email", unique = true)
    private String email;
    
    @Column(name = "password_hash")
    private String passwordHash;
    
    @Column(name = "display_name")
    private String displayName;
    
    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;
    
    @Column(name = "avatar_url")
    private String avatarUrl;
    
    @Column(name = "website")
    private String website;
    
    @Column(name = "xiaohongshu")
    private String xiaohongshu;
    
    @Column(name = "location")
    private String location;
    
    @Column(name = "favorite_camera")
    private String favoriteCamera;
    
    @Column(name = "favorite_lens")
    private String favoriteLens;
    
    @Column(name = "favorite_photographer")
    private String favoritePhotographer;
    
    @Column(name = "is_active")
    private Boolean isActive;
    
    @Column(name = "user_tier")
    private String userTier; // FREE, BASIC, PRO, ENTERPRISE
    
    @Column(name = "storage_used")
    private Long storageUsed; // Bytes used
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
        if (userTier == null) {
            userTier = "FREE";
        }
        if (storageUsed == null) {
            storageUsed = 0L;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
