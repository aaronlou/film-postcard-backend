package world.isnap.filmpostcard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePostcardFromImageRequest {
    private String imageFilename;  // The filename returned from /api/upload
    private String textContent;
    private String templateType;
    private String qrUrl;
}
