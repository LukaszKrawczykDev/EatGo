package pollub.eatgo.dto.order;

import pollub.eatgo.dto.restaurant.RestaurantSummaryDto;

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
