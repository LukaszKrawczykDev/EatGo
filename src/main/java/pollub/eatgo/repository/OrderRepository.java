package pollub.eatgo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pollub.eatgo.model.Order;
import pollub.eatgo.model.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Order> findByIdAndUserId(Long id, Long userId);
    List<Order> findByRestaurantId(Long restaurantId);
	List<Order> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId);
    Optional<Order> findByIdAndRestaurantId(Long id, Long restaurantId);
	List<Order> findByCourierIdOrderByCreatedAtDesc(Long courierId);
	Optional<Order> findByIdAndCourierId(Long id, Long courierId);
	List<Order> findByRestaurantIdAndCreatedAtAfter(Long restaurantId, LocalDateTime date);
	long countByRestaurantIdAndCreatedAtAfter(Long restaurantId, LocalDateTime date);
	long countByRestaurantIdAndStatusIn(Long restaurantId, List<OrderStatus> statuses);
	boolean existsByCourierIdAndStatus(Long courierId, OrderStatus status);
}
