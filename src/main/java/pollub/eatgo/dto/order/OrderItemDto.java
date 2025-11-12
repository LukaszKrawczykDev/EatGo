package pollub.eatgo.dto.order;

public record OrderItemDto(
        Long id,
        Long dishId,
        String dishName,
        int quantity,
        double priceSnapshot
) {}