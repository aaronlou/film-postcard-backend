package world.isnap.filmpostcard.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import world.isnap.filmpostcard.dto.OrderRequest;
import world.isnap.filmpostcard.dto.OrderResponse;
import world.isnap.filmpostcard.entity.Order;
import world.isnap.filmpostcard.repository.OrderRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {
    
    private final OrderRepository orderRepository;
    
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        // Validate request
        validateOrderRequest(request);
        
        // Create order entity
        Order order = Order.builder()
                .postcardId(request.getPostcardId())
                .customerName(request.getName())
                .customerPhone(request.getPhone())
                .deliveryAddress(request.getAddress())
                .quantity(request.getQuantity())
                .build();
        
        Order saved = orderRepository.save(order);
        log.info("Order created with ID: {}", saved.getId());
        
        return toResponse(saved);
    }
    
    public OrderResponse getOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
        return toResponse(order);
    }
    
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    public List<OrderResponse> getOrdersByPostcardId(Long postcardId) {
        return orderRepository.findByPostcardIdOrderByCreatedAtDesc(postcardId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    private void validateOrderRequest(OrderRequest request) {
        if (request.getPostcardId() == null) {
            throw new RuntimeException("Postcard ID is required");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new RuntimeException("Customer name is required");
        }
        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            throw new RuntimeException("Customer phone is required");
        }
        if (request.getAddress() == null || request.getAddress().trim().isEmpty()) {
            throw new RuntimeException("Delivery address is required");
        }
        if (request.getQuantity() == null || request.getQuantity() < 1) {
            throw new RuntimeException("Quantity must be at least 1");
        }
    }
    
    private OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .postcardId(order.getPostcardId())
                .customerName(order.getCustomerName())
                .customerPhone(order.getCustomerPhone())
                .deliveryAddress(order.getDeliveryAddress())
                .quantity(order.getQuantity())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
