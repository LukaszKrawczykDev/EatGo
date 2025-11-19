package pollub.eatgo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import pollub.eatgo.dto.auth.AuthDto;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class AuthService {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private static final String API_BASE_URL = "http://localhost:8080/api/auth";
    
    public AuthService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    public AuthResult login(String email, String password) {
        try {
            AuthDto.LoginRequest request = new AuthDto.LoginRequest(email, password);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<AuthDto.LoginRequest> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<AuthDto.AuthResponse> response = restTemplate.exchange(
                    API_BASE_URL + "/login",
                    HttpMethod.POST,
                    entity,
                    AuthDto.AuthResponse.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                AuthDto.AuthResponse authResponse = response.getBody();
                return new AuthResult(true, authResponse.getToken(), authResponse.getUserId(), authResponse.getRole(), null);
            }
            
            return new AuthResult(false, null, null, null, "Nieprawidłowe dane logowania");
        } catch (HttpClientErrorException e) {
            log.error("HTTP error during login: {}", e.getStatusCode(), e);
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                return new AuthResult(false, null, null, null, "Nieprawidłowy email lub hasło");
            }
            String errorBody = e.getResponseBodyAsString();
            return new AuthResult(false, null, null, null, errorBody != null && !errorBody.isBlank() ? errorBody : "Błąd podczas logowania");
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("Connection error during login", e);
            return new AuthResult(false, null, null, null, "Nie można połączyć się z serwerem. Sprawdź połączenie internetowe.");
        } catch (Exception e) {
            log.error("Unexpected error during login", e);
            return new AuthResult(false, null, null, null, "Wystąpił błąd podczas logowania: " + e.getMessage());
        }
    }
    
    public AuthResult register(String email, String password, String fullName, String role,
                               String restaurantName, String restaurantAddress, Double restaurantDeliveryPrice) {
        try {
            AuthDto.RegisterRequest request = new AuthDto.RegisterRequest();
            request.setEmail(email);
            request.setPassword(password);
            request.setFullName(fullName);
            request.setRole(role);
            
            if ("RESTAURANT_ADMIN".equals(role)) {
                request.setRestaurantName(restaurantName);
                request.setRestaurantAddress(restaurantAddress);
                request.setRestaurantDeliveryPrice(restaurantDeliveryPrice);
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<AuthDto.RegisterRequest> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<AuthDto.AuthResponse> response = restTemplate.exchange(
                    API_BASE_URL + "/register",
                    HttpMethod.POST,
                    entity,
                    AuthDto.AuthResponse.class
            );
            
            if (response.getStatusCode() == HttpStatus.CREATED && response.getBody() != null) {
                AuthDto.AuthResponse authResponse = response.getBody();
                return new AuthResult(true, authResponse.getToken(), authResponse.getUserId(), authResponse.getRole(), null);
            }
            
            return new AuthResult(false, null, null, null, "Błąd podczas rejestracji");
        } catch (HttpClientErrorException e) {
            log.error("HTTP error during registration: {}", e.getStatusCode(), e);
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                return new AuthResult(false, null, null, null, "Email jest już w użyciu");
            }
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                String errorMessage = e.getResponseBodyAsString();
                return new AuthResult(false, null, null, null, errorMessage != null && !errorMessage.isBlank() ? errorMessage : "Nieprawidłowe dane rejestracji");
            }
            String errorBody = e.getResponseBodyAsString();
            return new AuthResult(false, null, null, null, errorBody != null && !errorBody.isBlank() ? errorBody : "Błąd podczas rejestracji");
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("Connection error during registration", e);
            return new AuthResult(false, null, null, null, "Nie można połączyć się z serwerem. Sprawdź połączenie internetowe.");
        } catch (Exception e) {
            log.error("Unexpected error during registration", e);
            return new AuthResult(false, null, null, null, "Wystąpił błąd podczas rejestracji: " + e.getMessage());
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

