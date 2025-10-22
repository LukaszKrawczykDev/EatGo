package pollub.eatgo.dto.client;

import java.math.BigDecimal;

public record RestaurantSummaryDto(
        Long id,
        String name,
        String address,
        BigDecimal deliveryPrice
) {}
