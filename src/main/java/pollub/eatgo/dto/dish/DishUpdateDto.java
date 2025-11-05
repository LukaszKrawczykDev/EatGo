package pollub.eatgo.dto.dish;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record DishUpdateDto(
        @NotBlank String name,
        @Size(max = 2000) String description,
        @PositiveOrZero Double price,
        Boolean available
) {}
