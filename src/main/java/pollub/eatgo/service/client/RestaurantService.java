package pollub.eatgo.service.client;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pollub.eatgo.dto.client.DishDto;
import pollub.eatgo.dto.client.RestaurantSummaryDto;
import pollub.eatgo.model.Dish;
import pollub.eatgo.model.Restaurant;
import pollub.eatgo.repository.DishRepository;
import pollub.eatgo.repository.RestaurantRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final DishRepository dishRepository;

    public List<RestaurantSummaryDto> listRestaurants() {
        return restaurantRepository.findAll().stream()
                .map(r -> new RestaurantSummaryDto(
                        r.getId(),
                        r.getName(),
                        r.getAddress(),
                        BigDecimal.valueOf(r.getDeliveryPrice())
                ))
                .toList();
    }

    public List<DishDto> getMenu(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));

        return dishRepository.findByRestaurantIdAndAvailableTrue(restaurant.getId()).stream()
                .map(d -> new DishDto(d.getId(), d.getName(), d.getDescription(), d.getPrice(), d.isAvailable()))
                .toList();
    }
}