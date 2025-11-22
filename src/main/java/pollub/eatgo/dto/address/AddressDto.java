package pollub.eatgo.dto.address;

public record AddressDto(
        Long id,
        String city,
        String street,
        String postalCode,
        String apartmentNumber
) {}
