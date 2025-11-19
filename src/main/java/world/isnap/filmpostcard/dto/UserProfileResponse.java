package world.isnap.filmpostcard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String username;
    private String email;
    private String displayName;
    private String bio;
    private String avatarUrl;
    private String website;
    private String xiaohongshu;
    private String location;
    private String favoriteCamera;
    private String favoriteLens;
    private String favoritePhotographer;
    private Long designCount;
    private Long photoCount;
    private LocalDateTime createdAt;
    
    // Storage quota fields
    private String userTier;           // FREE, BASIC, PRO
    private Long storageUsed;          // Bytes used
    private Long storageLimit;         // Total bytes allowed
    private Integer photoLimit;        // Max photo count
    private Long singleFileLimit;      // Max bytes per file
}
