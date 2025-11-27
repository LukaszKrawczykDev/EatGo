package pollub.eatgo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import pollub.eatgo.dto.auth.AuthDto;
import pollub.eatgo.model.User;
import pollub.eatgo.repository.UserRepository;
import pollub.eatgo.security.JwtUtil;
import pollub.eatgo.model.Restaurant;
import pollub.eatgo.repository.RestaurantRepository;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
	private final RestaurantRepository restaurantRepository;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid AuthDto.RegisterRequest body) {
        Optional<User> exists = userRepository.findByEmail(body.getEmail());
        if (exists.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already in use");
        }

		if (body.getRole() == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Role is required");
		}
		User.Role role;
		try {
			role = User.Role.valueOf(body.getRole().toUpperCase());
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid role. Use CLIENT or RESTAURANT_ADMIN");
        }

        User user = User.builder()
                .email(body.getEmail())
                .password(passwordEncoder.encode(body.getPassword()))
                .fullName(body.getFullName())
				.role(role)
                .build();

        user = userRepository.save(user);

		if (role == User.Role.RESTAURANT_ADMIN) {
			if (body.getRestaurantName() == null || body.getRestaurantName().isBlank()
					|| body.getRestaurantAddress() == null || body.getRestaurantAddress().isBlank()) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("restaurantName and restaurantAddress are required for RESTAURANT_ADMIN");
			}
			double delivery = body.getRestaurantDeliveryPrice() == null ? 0.0 : body.getRestaurantDeliveryPrice();
			if (delivery < 0) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("restaurantDeliveryPrice must be >= 0");
			}
			Restaurant restaurant = Restaurant.builder()
					.name(body.getRestaurantName())
					.address(body.getRestaurantAddress())
					.deliveryPrice(delivery)
					.admin(user)
					.build();
			restaurantRepository.save(restaurant);
		}

        String token = jwtUtil.generateToken(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthDto.AuthResponse(token, user.getId(), user.getRole().name()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid AuthDto.LoginRequest body) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(body.getEmail(), body.getPassword())
            );
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }

        User user = userRepository.findByEmail(body.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found after authentication"));

        String token = jwtUtil.generateToken(user);

        return ResponseEntity.ok(new AuthDto.AuthResponse(token, user.getId(), user.getRole().name()));
    }
    
    @PutMapping("/password")
    public ResponseEntity<?> changePassword(
            @RequestBody @Valid AuthDto.ChangePasswordRequest body,
            org.springframework.security.core.Authentication auth) {
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, body.getOldPassword())
            );
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Nieprawidłowe obecne hasło");
        }
        

        user.setPassword(passwordEncoder.encode(body.getNewPassword()));
        userRepository.save(user);
        
        return ResponseEntity.ok("Hasło zostało zmienione pomyślnie");
    }
}
