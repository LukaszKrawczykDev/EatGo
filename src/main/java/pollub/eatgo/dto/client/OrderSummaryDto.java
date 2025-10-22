package pollub.eatgo.dto.client;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record OrderSummaryDto(
        Long id,
        String status,
        BigDecimal totalPrice,
        BigDecimal deliveryPrice,
        OffsetDateTime createdAt,
        RestaurantSummaryDto restaurant
) {}
