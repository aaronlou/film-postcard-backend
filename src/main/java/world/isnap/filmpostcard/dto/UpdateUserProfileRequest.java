package world.isnap.filmpostcard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserProfileRequest {
    private String displayName;
    private String bio;
    private String website;
    private String xiaohongshu;
    private String location;
}
