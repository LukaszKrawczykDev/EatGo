package pollub.eatgo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import pollub.eatgo.dto.courier.CourierCreateDto;
import pollub.eatgo.dto.courier.CourierDto;
import pollub.eatgo.dto.dish.DishCreateDto;
import pollub.eatgo.dto.dish.DishDto;
import pollub.eatgo.dto.dish.DishUpdateDto;
import pollub.eatgo.dto.order.*;
import pollub.eatgo.dto.restaurant.RestaurantDto;
import pollub.eatgo.dto.restaurant.RestaurantUpdateDto;
import pollub.eatgo.dto.review.ReviewDto;
import pollub.eatgo.service.RestaurantService;
import pollub.eatgo.service.ReviewService;

import java.util.List;

@RestController
@RequestMapping({"/api/restaurant", "/api/admin"})
@RequiredArgsConstructor
@Validated
public class RestaurantController {

    private final RestaurantService restaurantService;
    private final ReviewService reviewService;

    @GetMapping("/orders")
    public ResponseEntity<List<OrderDto>> listOrders(Authentication auth) {
        String email = auth.getName();
        return ResponseEntity.ok(restaurantService.listOrders(email));
    }

    @PutMapping("/orders/{id}/status")
    public ResponseEntity<OrderDto> updateOrderStatus(Authentication auth,
                                                      @PathVariable Long id,
                                                      @RequestBody @Valid OrderStatusUpdateDto body) {
        String email = auth.getName();
        return ResponseEntity.ok(restaurantService.updateOrderStatus(email, id, body.status()));
    }

    @PutMapping("/orders/{id}/courier")
    public ResponseEntity<OrderDto> assignCourier(Authentication auth,
                                                  @PathVariable Long id,
                                                  @RequestBody @Valid AssignCourierDto body) {
        String email = auth.getName();
        return ResponseEntity.ok(restaurantService.assignCourier(email, id, body.courierId()));
    }

    @PostMapping("/dishes")
    public ResponseEntity<DishDto> addDish(Authentication auth,
                                           @RequestBody @Valid DishCreateDto body) {
        String email = auth.getName();
        return ResponseEntity.ok(restaurantService.addDish(email, body));
    }

    @PutMapping("/dishes/{id}")
    public ResponseEntity<DishDto> updateDish(Authentication auth,
                                              @PathVariable Long id,
                                              @RequestBody @Valid DishUpdateDto body) {
        String email = auth.getName();
        return ResponseEntity.ok(restaurantService.updateDish(email, id, body));
    }

    @DeleteMapping("/dishes/{id}")
    public ResponseEntity<Void> deleteDish(Authentication auth, @PathVariable Long id) {
        String email = auth.getName();
        restaurantService.deleteDish(email, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/couriers")
    public ResponseEntity<List<CourierDto>> listCouriers(Authentication auth) {
        String email = auth.getName();
        return ResponseEntity.ok(restaurantService.listCouriers(email));
    }

    @PostMapping("/couriers")
    public ResponseEntity<CourierDto> createCourier(Authentication auth, @RequestBody @Valid CourierCreateDto body) {
        String email = auth.getName();
        return ResponseEntity.ok(restaurantService.createCourier(email, body));
    }

    @DeleteMapping("/couriers/{id}")
    public ResponseEntity<Void> deleteCourier(Authentication auth, @PathVariable Long id) {
        String email = auth.getName();
        restaurantService.deleteCourier(email, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reviews")
    public ResponseEntity<List<ReviewDto>> listReviews(Authentication auth) {
        String email = auth.getName();
        return ResponseEntity.ok(reviewService.getReviewsForAdmin(email));
    }

    @PutMapping("/restaurant")
    public ResponseEntity<RestaurantDto> updateRestaurant(Authentication auth, @RequestBody @Valid RestaurantUpdateDto body) {
        String email = auth.getName();
        return ResponseEntity.ok(restaurantService.updateRestaurant(email, body));
    }
}
