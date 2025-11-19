package pollub.eatgo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pollub.eatgo.dto.address.AddressCreateDto;
import pollub.eatgo.dto.address.AddressDto;
import pollub.eatgo.model.Address;
import pollub.eatgo.model.User;
import pollub.eatgo.repository.AddressRepository;
import pollub.eatgo.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
	private final UserRepository userRepository;

    public AddressDto addAddress(Long userId, AddressCreateDto dto) {
		User user = userRepository.findById(userId).orElseThrow();
        Address address = new Address();
        address.setCity(dto.city());
        address.setStreet(dto.street());
        address.setPostalCode(dto.postalCode());
		address.setUser(user);
        Address saved = addressRepository.save(address);
        return new AddressDto(saved.getId(), saved.getCity(), saved.getStreet(), saved.getPostalCode());
    }

    public List<AddressDto> listAddresses(Long userId) {
        return addressRepository.findByUserIdOrderByIdDesc(userId).stream()
                .map(a -> new AddressDto(a.getId(), a.getCity(), a.getStreet(), a.getPostalCode()))
                .toList();
    }
}