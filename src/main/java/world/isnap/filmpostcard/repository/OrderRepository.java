package world.isnap.filmpostcard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import world.isnap.filmpostcard.entity.Order;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    List<Order> findAllByOrderByCreatedAtDesc();
    
    List<Order> findByPostcardIdOrderByCreatedAtDesc(Long postcardId);
}
