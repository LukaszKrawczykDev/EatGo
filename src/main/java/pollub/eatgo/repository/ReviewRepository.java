package pollub.eatgo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pollub.eatgo.model.Review;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByOrderIdAndReviewerIdAndTargetType(Long orderId, Long reviewerId, String targetType);
	List<Review> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, Long targetId);
}
