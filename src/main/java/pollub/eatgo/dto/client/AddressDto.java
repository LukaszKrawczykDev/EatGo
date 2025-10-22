package pollub.eatgo.dto.client;

public record AddressDto(
        Long id,
        String city,
        String street,
        String postalCode
) {}
