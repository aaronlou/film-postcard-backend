package world.isnap.filmpostcard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlbumResponse {
    private String id;
    private String name;
    private String description;
    private String coverPhoto;
    private Integer photoCount;
    private String createdAt;
    private String updatedAt;
}
