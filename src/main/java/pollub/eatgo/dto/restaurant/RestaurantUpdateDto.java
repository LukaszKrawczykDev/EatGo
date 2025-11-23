// file: pollub/eatgo/dto/restaurant/RestaurantUpdateDto.java
package pollub.eatgo.dto.restaurant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record RestaurantUpdateDto(
        @NotBlank String name,
        @NotBlank String address,
        @PositiveOrZero Double deliveryPrice,
        String imageUrl
) {}
