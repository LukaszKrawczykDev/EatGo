package pollub.eatgo.dto.courier;

import jakarta.validation.constraints.NotBlank;

public record CourierUpdateDto(
        @NotBlank String email,
        @NotBlank String fullName
) {}

