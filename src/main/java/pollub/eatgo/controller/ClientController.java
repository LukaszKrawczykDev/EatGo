package pollub.eatgo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pollub.eatgo.dto.address.AddressCreateDto;
import pollub.eatgo.dto.address.AddressDto;
import pollub.eatgo.dto.dish.DishDto;
import pollub.eatgo.dto.restaurant.RestaurantSummaryDto;
import pollub.eatgo.model.User;
import pollub.eatgo.repository.UserRepository;
import pollub.eatgo.service.AddressService;
import pollub.eatgo.service.RestaurantService;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ClientController {

    private final RestaurantService restaurantService;
    private final AddressService addressService;
    private final UserRepository userRepository;

    @GetMapping("/restaurants")
    public List<RestaurantSummaryDto> getRestaurants() {
        return restaurantService.listRestaurants();
    }

    @GetMapping("/restaurants/{id}/menu")
    public List<DishDto> getMenu(@PathVariable Long id) {
        return restaurantService.getMenu(id);
    }

    @PostMapping("/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    public AddressDto addAddress(@RequestBody AddressCreateDto dto, Authentication auth) {
        Long userId = getUserId(auth);
        return addressService.addAddress(userId, dto);
    }

    @GetMapping("/addresses")
    public List<AddressDto> getAddresses(Authentication auth) {
        Long userId = getUserId(auth);
        return addressService.listAddresses(userId);
    }

    private Long getUserId(Authentication auth) {
        if (auth == null || auth.getName() == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");

        String email = auth.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        return user.getId();
    }
}