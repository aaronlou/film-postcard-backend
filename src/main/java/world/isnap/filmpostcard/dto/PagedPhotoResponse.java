package world.isnap.filmpostcard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedPhotoResponse {
    private List<PhotoResponse> photos;
    private int currentPage;      // 当前页码 (从1开始)
    private int pageSize;          // 每页数量
    private long totalPhotos;      // 总照片数
    private int totalPages;        // 总页数
    private boolean hasNext;       // 是否有下一页
    private boolean hasPrevious;   // 是否有上一页
}
