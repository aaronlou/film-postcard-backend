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
public class PhotoResponse {
    private String id;  // String for frontend compatibility
    private String imageUrl;
    private String title;
    private String description;
    private String location;
    private String camera;
    private String lens;
    private String settings;
    private String takenAt;
    private String createdAt;
}
