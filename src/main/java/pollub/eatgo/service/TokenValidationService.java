package pollub.eatgo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pollub.eatgo.security.JwtUtil;

/**
 * Serwis do walidacji tokenów JWT po stronie frontendu.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenValidationService {
    
    private final JwtUtil jwtUtil;

    public boolean isTokenValid(String token) {
        if (token == null || token.isBlank() || "null".equals(token)) {
            return false;
        }
        
        try {
            return jwtUtil.validateToken(token);
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        if (token == null || token.isBlank() || "null".equals(token)) {
            return true;
        }
        
        try {
            var claims = jwtUtil.getAllClaims(token);
            return claims.getExpiration().before(new java.util.Date());
        } catch (Exception e) {
            log.warn("Token expiration check failed: {}", e.getMessage());
            return true;
        }
    }

    public TokenInfo getTokenInfo(String token) {
        if (!isTokenValid(token)) {
            return null;
        }
        
        try {
            String userId = jwtUtil.extractUserId(token);
            String role = jwtUtil.extractRole(token);
            String email = jwtUtil.extractEmail(token);
            
            return new TokenInfo(userId, role, email);
        } catch (Exception e) {
            log.error("Error extracting token info: {}", e.getMessage());
            return null;
        }
    }
    
    public static class TokenInfo {
        private final String userId;
        private final String role;
        private final String email;
        
        public TokenInfo(String userId, String role, String email) {
            this.userId = userId;
            this.role = role;
            this.email = email;
        }
        
        public String getUserId() {
            return userId;
        }
        
        public String getRole() {
            return role;
        }
        
        public String getEmail() {
            return email;
        }
    }
}

