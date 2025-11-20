package world.isnap.filmpostcard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageUploadResponse {
    private String id;
    private String url;           // Original image URL
    private String urlThumb;      // Thumbnail URL
    private String urlMedium;     // Medium preview URL
    private String filename;
    private Long fileSize;
}
