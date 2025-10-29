package pollub.eatgo.dto.dish;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record DishCreateDto(
        @NotBlank String name,
        @Size(max = 2000) String description,
        @NotNull @PositiveOrZero Double price
) {}
