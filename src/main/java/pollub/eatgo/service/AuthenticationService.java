package pollub.eatgo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pollub.eatgo.model.Restaurant;
import pollub.eatgo.model.User;
import pollub.eatgo.repository.RestaurantRepository;
import pollub.eatgo.repository.UserRepository;
import pollub.eatgo.security.JwtUtil;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RestaurantRepository restaurantRepository;
    
    public AuthResult login(String email, String password) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );
            
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found after authentication"));
            
            String token = jwtUtil.generateToken(user);
            
            return new AuthResult(true, token, user.getId(), user.getRole().name(), null);
        } catch (AuthenticationException e) {
            log.error("Authentication failed for email: {}", email, e);
            return new AuthResult(false, null, null, null, "Nieprawidłowy email lub hasło");
        } catch (Exception e) {
            log.error("Unexpected error during login", e);
            return new AuthResult(false, null, null, null, "Wystąpił błąd podczas logowania: " + e.getMessage());
        }
    }
    
    @Transactional
    public AuthResult register(String email, String password, String fullName, String role,
                               String restaurantName, String restaurantAddress, Double restaurantDeliveryPrice) {
        try {
            Optional<User> exists = userRepository.findByEmail(email);
            if (exists.isPresent()) {
                return new AuthResult(false, null, null, null, "Email jest już w użyciu");
            }
            
            if (role == null || role.isBlank()) {
                return new AuthResult(false, null, null, null, "Rola jest wymagana");
            }
            
            User.Role userRole;
            try {
                userRole = User.Role.valueOf(role.toUpperCase());
            } catch (IllegalArgumentException e) {
                return new AuthResult(false, null, null, null, "Nieprawidłowa rola. Użyj CLIENT lub RESTAURANT_ADMIN");
            }
            
            User user = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .fullName(fullName)
                    .role(userRole)
                    .build();
            
            user = userRepository.save(user);
            
            if (userRole == User.Role.RESTAURANT_ADMIN) {
                if (restaurantName == null || restaurantName.isBlank() ||
                    restaurantAddress == null || restaurantAddress.isBlank()) {
                    return new AuthResult(false, null, null, null, 
                        "Nazwa restauracji i adres są wymagane dla RESTAURANT_ADMIN");
                }
                
                double delivery = restaurantDeliveryPrice == null ? 0.0 : restaurantDeliveryPrice;
                if (delivery < 0) {
                    return new AuthResult(false, null, null, null, 
                        "Cena dostawy musi być >= 0");
                }
                
                Restaurant restaurant = Restaurant.builder()
                        .name(restaurantName)
                        .address(restaurantAddress)
                        .deliveryPrice(delivery)
                        .admin(user)
                        .build();
                
                restaurantRepository.save(restaurant);
            }
            
            String token = jwtUtil.generateToken(user);
            
            return new AuthResult(true, token, user.getId(), user.getRole().name(), null);
        } catch (Exception e) {
            log.error("Error during registration", e);
            return new AuthResult(false, null, null, null, 
                "Wystąpił błąd podczas rejestracji: " + e.getMessage());
        }
    }
    
    public static class AuthResult {
        private final boolean success;
        private final String token;
        private final Long userId;
        private final String role;
        private final String errorMessage;
        
        public AuthResult(boolean success, String token, Long userId, String role, String errorMessage) {
            this.success = success;
            this.token = token;
            this.userId = userId;
            this.role = role;
            this.errorMessage = errorMessage;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getToken() {
            return token;
        }
        
        public Long getUserId() {
            return userId;
        }
        
        public String getRole() {
            return role;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
}

