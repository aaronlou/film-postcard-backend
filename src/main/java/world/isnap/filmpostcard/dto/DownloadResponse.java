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
public class DownloadResponse {
    private Long id;
    private String imageId;
    private String templateType;
    private LocalDateTime downloadTimestamp;
    private LocalDateTime createdAt;
}
