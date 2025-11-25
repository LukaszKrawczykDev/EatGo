package pollub.eatgo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pollub.eatgo.dto.user.UserSettingsDto;
import pollub.eatgo.model.User;
import pollub.eatgo.repository.UserRepository;
import pollub.eatgo.service.UserService;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    private final UserRepository userRepository;
    
    @GetMapping("/settings")
    public ResponseEntity<UserSettingsDto> getUserSettings(Authentication auth) {
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        UserSettingsDto settings = userService.getUserSettings(user.getId());
        return ResponseEntity.ok(settings);
    }
    
    @PutMapping("/settings")
    public ResponseEntity<UserSettingsDto> updateUserSettings(
            @RequestBody @Valid UserSettingsDto settings,
            Authentication auth) {
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        UserSettingsDto updatedSettings = userService.updateUserSettings(user.getId(), settings);
        return ResponseEntity.ok(updatedSettings);
    }
}

