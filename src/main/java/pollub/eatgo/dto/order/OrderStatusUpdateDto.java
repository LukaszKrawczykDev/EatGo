package pollub.eatgo.dto.order;

import jakarta.validation.constraints.NotBlank;

public record OrderStatusUpdateDto(
        @NotBlank String status
) {}
