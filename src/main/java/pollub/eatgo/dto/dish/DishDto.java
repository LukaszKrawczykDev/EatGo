package pollub.eatgo.dto.dish;

public record DishDto(
        Long id,
        String name,
        String description,
        double price,
        boolean available,
        Long restaurantId,
        String category,
        String imageUrl
) {}