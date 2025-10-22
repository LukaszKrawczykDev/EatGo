package pollub.eatgo.service.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pollub.eatgo.dto.client.AddressCreateDto;
import pollub.eatgo.dto.client.AddressDto;
import pollub.eatgo.model.Address;
import pollub.eatgo.repository.AddressRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;

    public AddressDto addAddress(Long userId, AddressCreateDto dto) {
        Address address = new Address();
        address.setId(userId);
        address.setCity(dto.city());
        address.setStreet(dto.street());
        address.setPostalCode(dto.postalCode());
        Address saved = addressRepository.save(address);
        return new AddressDto(saved.getId(), saved.getCity(), saved.getStreet(), saved.getPostalCode());
    }

    public List<AddressDto> listAddresses(Long userId) {
        return addressRepository.findByUserIdOrderByIdDesc(userId).stream()
                .map(a -> new AddressDto(a.getId(), a.getCity(), a.getStreet(), a.getPostalCode()))
                .toList();
    }
}