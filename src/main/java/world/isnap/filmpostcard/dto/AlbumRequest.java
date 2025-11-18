package world.isnap.filmpostcard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlbumRequest {
    private String name;
    private String description;
    private String coverPhoto;
}
