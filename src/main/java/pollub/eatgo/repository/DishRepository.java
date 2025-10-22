package pollub.eatgo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pollub.eatgo.model.Dish;

import java.util.List;

public interface DishRepository extends JpaRepository<Dish, Long> {
    List<Dish> findByRestaurantIdAndAvailableTrue(Long restaurantId);
}
