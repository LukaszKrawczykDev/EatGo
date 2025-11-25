package pollub.eatgo.dto.user;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserSettingsDto {
    private String defaultCity;
    private Long defaultAddressId;
    
    @Pattern(regexp = "light|dark", message = "Theme must be 'light' or 'dark'")
    private String theme;
}

