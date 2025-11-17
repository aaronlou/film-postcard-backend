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
public class OrderResponse {
    private Long id;
    private Long postcardId;
    private String customerName;
    private String customerPhone;
    private String deliveryAddress;
    private Integer quantity;
    private String status;
    private LocalDateTime createdAt;
}
