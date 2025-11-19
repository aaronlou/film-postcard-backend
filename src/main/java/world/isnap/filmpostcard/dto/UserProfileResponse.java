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
}
