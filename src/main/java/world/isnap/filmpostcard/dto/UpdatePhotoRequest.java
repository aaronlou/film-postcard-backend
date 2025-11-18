package world.isnap.filmpostcard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePhotoRequest {
    private String albumId; // null means remove from album
    private String title;
    private String description;
    private String location;
    private String camera;
    private String lens;
    private String settings;
    private String takenAt;
}
