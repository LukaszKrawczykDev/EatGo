package pollub.eatgo.dto.order;

import java.math.BigDecimal;

public record OrderItemDto(
        Long id,
        Long dishId,
        String dishName,
        int quantity,
        double priceSnapshot
) {}