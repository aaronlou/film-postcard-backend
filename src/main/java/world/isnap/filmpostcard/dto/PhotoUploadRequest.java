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
    private String imageUrl;         // Original image URL
    private String imageUrlThumb;    // Thumbnail URL
    private String imageUrlMedium;   // Medium preview URL
    private String title;
    private String description;
    private String location;
    private String camera;
    private String lens;
    private String settings;
    private String takenAt; // ISO 8601 format
    private String albumId; // Album ID for categorization
}
