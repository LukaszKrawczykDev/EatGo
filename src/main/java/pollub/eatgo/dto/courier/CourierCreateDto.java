package pollub.eatgo.dto.courier;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CourierCreateDto(
        @Email @NotBlank String email,
        @NotBlank String fullName,
        @NotBlank @Size(min = 6, max = 255) String password
) {}
