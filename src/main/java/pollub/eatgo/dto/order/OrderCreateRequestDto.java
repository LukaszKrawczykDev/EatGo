package pollub.eatgo.dto.order;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OrderCreateRequestDto(
        @NotNull Long restaurantId,
        @NotNull Long addressId,
        @NotEmpty List<OrderItemRequestDto> items
) {}
