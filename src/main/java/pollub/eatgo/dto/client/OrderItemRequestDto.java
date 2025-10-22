package pollub.eatgo.dto.client;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderItemRequestDto(
        @NotNull Long dishId,
        @Min(1) int quantity
) {}
