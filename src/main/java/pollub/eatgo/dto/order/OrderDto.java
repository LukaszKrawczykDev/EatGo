package pollub.eatgo.dto.order;

import java.time.LocalDateTime;
import java.util.List;

public record OrderDto(
        Long id,
        String status,
        double totalPrice,
        double deliveryPrice,
        LocalDateTime createdAt,
        List<OrderItemDto> items,
        Long userId,
        String userEmail,
        Long courierId,
        String courierEmail
) {}
