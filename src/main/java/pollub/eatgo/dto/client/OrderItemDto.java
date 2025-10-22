package pollub.eatgo.dto.client;

import java.math.BigDecimal;

public record OrderItemDto(
        Long dishId,
        String dishName,
        int quantity,
        BigDecimal priceSnapshot
) {}
