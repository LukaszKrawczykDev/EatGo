package pollub.eatgo.dto.client;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record OrderDetailsDto(
        Long id,
        String status,
        BigDecimal totalPrice,
        BigDecimal deliveryPrice,
        OffsetDateTime createdAt,
        RestaurantSummaryDto restaurant,
        List<OrderItemDto> items,
        AddressDto deliveryAddress
) {}
