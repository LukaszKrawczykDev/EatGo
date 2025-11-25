package pollub.eatgo.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pollub.eatgo.dto.user.UserSettingsDto;
import pollub.eatgo.model.Address;
import pollub.eatgo.model.User;
import pollub.eatgo.repository.AddressRepository;
import pollub.eatgo.repository.UserRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    
    public UserSettingsDto getUserSettings(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return new UserSettingsDto(
                user.getDefaultCity(),
                user.getDefaultAddress() != null ? user.getDefaultAddress().getId() : null,
                user.getTheme() != null ? user.getTheme() : "light"
        );
    }
    
    public UserSettingsDto updateUserSettings(Long userId, UserSettingsDto settings) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setDefaultCity(settings.getDefaultCity());
        user.setTheme(settings.getTheme() != null ? settings.getTheme() : "light");
        
        // Ustaw domyślny adres jeśli został podany
        if (settings.getDefaultAddressId() != null) {
            Optional<Address> address = addressRepository.findById(settings.getDefaultAddressId());
            if (address.isPresent() && address.get().getUser().getId().equals(userId)) {
                user.setDefaultAddress(address.get());
            } else {
                throw new RuntimeException("Address not found or does not belong to user");
            }
        } else {
            user.setDefaultAddress(null);
        }
        
        user = userRepository.save(user);
        
        return new UserSettingsDto(
                user.getDefaultCity(),
                user.getDefaultAddress() != null ? user.getDefaultAddress().getId() : null,
                user.getTheme()
        );
    }
}
