package world.isnap.filmpostcard.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "photos", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "image_url"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Photo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id")
    private Album album;
    
    @Column(name = "image_url", nullable = false)
    private String imageUrl;  // Original image URL
    
    @Column(name = "image_url_thumb")
    private String imageUrlThumb;  // Thumbnail (300px width, ~50KB)
    
    @Column(name = "image_url_medium")
    private String imageUrlMedium;  // Medium preview (1280px width, ~200-500KB)
    
    @Column(name = "title")
    private String title;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "location")
    private String location;
    
    @Column(name = "camera")
    private String camera;
    
    @Column(name = "lens")
    private String lens;
    
    @Column(name = "settings")
    private String settings; // ISO, aperture, shutter speed
    
    @Column(name = "taken_at")
    private LocalDateTime takenAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
