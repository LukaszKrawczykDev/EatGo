package pollub.eatgo.dto.address;

import jakarta.validation.constraints.NotBlank;

public record AddressCreateDto(
        @NotBlank String city,
        @NotBlank String street,
        @NotBlank String postalCode,
        String apartmentNumber
) {}
