package pollub.eatgo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pollub.eatgo.model.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByOrderIdAndReviewerIdAndTargetType(Long orderId, Long reviewerId, String targetType);
}
