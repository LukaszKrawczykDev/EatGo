package pollub.eatgo.dto.client;

import jakarta.validation.constraints.NotBlank;

public record AddressCreateDto(
        @NotBlank String city,
        @NotBlank String street,
        @NotBlank String postalCode
) {}
