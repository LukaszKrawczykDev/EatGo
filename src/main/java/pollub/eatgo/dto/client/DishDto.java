package pollub.eatgo.dto.client;

public record DishDto(
        Long id,
        String name,
        String description,
        @jakarta.validation.constraints.Positive double price,
        boolean available
) {}
