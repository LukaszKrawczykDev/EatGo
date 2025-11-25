package pollub.eatgo.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AuthDto {
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RegisterRequest {
        @Email
        @NotBlank
        private String email;
        @NotBlank
        private String password;
        @NotBlank
        private String fullName;
		@NotBlank
		private String role; // CLIENT albo RESTAURANT_ADMIN
		// Pola wymagane tylko dla RESTAURANT_ADMIN:
		private String restaurantName;
		private String restaurantAddress;
		private Double restaurantDeliveryPrice;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LoginRequest {
        @Email
        @NotBlank
        private String email;
        @NotBlank
        private String password;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AuthResponse {
        private String token;
        private Long userId;
        private String role;
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChangePasswordRequest {
        @NotBlank
        private String oldPassword;
        @NotBlank
        private String newPassword;
    }
}
