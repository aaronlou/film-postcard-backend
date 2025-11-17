package world.isnap.filmpostcard.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "downloads")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Download {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "image_id")
    private String imageId;  // Can be filename or postcard ID
    
    @Column(name = "template_type")
    private String templateType;
    
    @Column(name = "text_content", columnDefinition = "TEXT")
    private String textContent;
    
    @Column(name = "qr_url")
    private String qrUrl;
    
    @Column(name = "download_timestamp", nullable = false)
    private LocalDateTime downloadTimestamp;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (downloadTimestamp == null) {
            downloadTimestamp = createdAt;
        }
    }
}
