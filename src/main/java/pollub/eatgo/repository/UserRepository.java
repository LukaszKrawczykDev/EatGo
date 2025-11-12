package pollub.eatgo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pollub.eatgo.model.User;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findByRole(User.Role role);
	List<User> findByRestaurantIdAndRole(Long restaurantId, User.Role role);
	Optional<User> findByIdAndRestaurantId(Long courierId, Long restaurantId);
}
