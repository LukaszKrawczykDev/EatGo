package pollub.eatgo.dto.order;

import pollub.eatgo.dto.address.AddressDto;
import pollub.eatgo.dto.restaurant.RestaurantSummaryDto;

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
