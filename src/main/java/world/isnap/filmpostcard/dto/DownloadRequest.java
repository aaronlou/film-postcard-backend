package world.isnap.filmpostcard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DownloadRequest {
    private String imageId;
    private String templateType;
    private String text;
    private String qrUrl;
    private String timestamp;  // ISO 8601 format from frontend
}
