package world.isnap.filmpostcard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoUploadRequest {
    private String imageUrl;
    private String title;
    private String description;
    private String location;
    private String camera;
    private String lens;
    private String settings;
    private String takenAt; // ISO 8601 format
    private String albumId; // Album ID for categorization
}
