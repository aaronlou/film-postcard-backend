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
public class PostcardResponse {
    private Long id;
    private String imageUrl;
    private String textContent;
    private String originalFilename;
    private Long fileSize;
    private LocalDateTime createdAt;
}
