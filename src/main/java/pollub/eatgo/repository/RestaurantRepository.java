package pollub.eatgo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pollub.eatgo.model.*;

import java.util.List;
import java.util.Optional;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {}

