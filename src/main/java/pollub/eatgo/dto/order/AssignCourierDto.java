package pollub.eatgo.dto.order;

import jakarta.validation.constraints.NotNull;

public record AssignCourierDto(
        @NotNull Long courierId
) {}
