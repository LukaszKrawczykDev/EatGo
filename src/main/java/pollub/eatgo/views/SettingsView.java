package pollub.eatgo.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import pollub.eatgo.dto.address.AddressDto;
import pollub.eatgo.service.AddressService;
import pollub.eatgo.service.AuthenticationService;
import pollub.eatgo.service.OrderNotificationService;
import pollub.eatgo.service.TokenValidationService;
import pollub.eatgo.views.components.HeaderComponent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Route("settings")
@PageTitle("EatGo - Ustawienia")
public class SettingsView extends VerticalLayout {
    
    private final AuthenticationService authService;
    private final TokenValidationService tokenValidationService;
    private final AddressService addressService;
    
    private PasswordField oldPasswordField;
    private PasswordField newPasswordField;
    private PasswordField confirmPasswordField;
    private Div passwordStrengthIndicator;
    private ProgressBar passwordStrengthBar;
    private Div passwordRequirements;
    
    private static final int MIN_PASSWORD_LENGTH = 8;
    private ComboBox<AddressDto> defaultAddressComboBox;
    private ComboBox<String> defaultCityComboBox;
    private RadioButtonGroup<String> themeRadioGroup;
    
    private List<AddressDto> userAddresses = new ArrayList<>();
    private static final List<String> AVAILABLE_CITIES = Arrays.asList("Warszawa", "Lublin", "Rzeszów");
    private boolean isInitializing = false;
    
    public SettingsView(AuthenticationService authService,
                        TokenValidationService tokenValidationService,
                        AddressService addressService,
                        OrderNotificationService orderNotificationService) {
        this.authService = authService;
        this.tokenValidationService = tokenValidationService;
        this.addressService = addressService;
        
        setSizeFull();
        setSpacing(false);
        setPadding(false);
        
        HeaderComponent headerComponent = new HeaderComponent(authService, tokenValidationService, orderNotificationService);
        add(headerComponent);
        
        Div content = new Div();
        content.addClassName("settings-content");
        content.getStyle().set("max-width", "800px");
        content.getStyle().set("margin", "0 auto");
        content.getStyle().set("padding", "2rem");
        
        H2 title = new H2("Ustawienia");
        title.addClassName("settings-title");

        Div passwordSection = createPasswordSection();

        Div defaultAddressSection = createDefaultAddressSection();

        Div defaultCitySection = createDefaultCitySection();

        Div themeSection = createThemeSection();
        
        content.add(title, passwordSection, defaultAddressSection, defaultCitySection, themeSection);
        add(content);
        
        loadSettings();
    }
    
    private void loadSettings() {
        isInitializing = true;

        getElement().executeJs(
            "const userId = localStorage.getItem('eatgo-userId'); " +
            "if (userId) { " +
            "  $0.$server.loadUserAddresses(userId); " +
            "}",
            getElement()
        );

        getElement().executeJs(
            "const token = localStorage.getItem('eatgo-token'); " +
            "if (!token) { " +
            "  $0.$server.onError('Musisz być zalogowany'); " +
            "  return; " +
            "} " +
            "fetch('/api/users/settings', { " +
            "  method: 'GET', " +
            "  headers: { " +
            "    'Authorization': 'Bearer ' + token, " +
            "    'Content-Type': 'application/json' " +
            "  } " +
            "}) " +
            ".then(r => { " +
            "  if (!r.ok) { " +
            "    throw new Error('Błąd podczas ładowania ustawień'); " +
            "  } " +
            "  return r.json(); " +
            "}) " +
            ".then(settings => { " +
            "  console.log('Settings loaded:', settings); " +
            "  $0.$server.loadSettingsFromApi(JSON.stringify(settings)); " +
            "}) " +
            ".catch(e => { " +
            "  console.error('Error loading settings:', e); " +
            "  $0.$server.onError('Błąd podczas ładowania ustawień: ' + e.message); " +
            "});",
            getElement()
        );
    }
    
    @com.vaadin.flow.component.ClientCallable
    public void loadSettingsFromApi(String settingsJson) {
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    com.fasterxml.jackson.databind.JsonNode settings = mapper.readTree(settingsJson);

                    if (settings.has("defaultCity") && !settings.get("defaultCity").isNull()) {
                        String city = settings.get("defaultCity").asText();
                        if (defaultCityComboBox != null && AVAILABLE_CITIES.contains(city)) {
                            defaultCityComboBox.setValue(city);
                        }
                    }

                    if (settings.has("defaultAddressId") && !settings.get("defaultAddressId").isNull()) {
                        Long addressId = settings.get("defaultAddressId").asLong();
                        if (defaultAddressComboBox != null) {
                            userAddresses.stream()
                                .filter(a -> a.id().equals(addressId))
                                .findFirst()
                                .ifPresent(defaultAddressComboBox::setValue);
                        }
                    }

                    String theme = settings.has("theme") && !settings.get("theme").isNull() 
                        ? settings.get("theme").asText() 
                        : "light";
                    if (themeRadioGroup != null && (theme.equals("light") || theme.equals("dark"))) {
                        themeRadioGroup.setValue(theme);
                    }

                    getElement().executeJs(
                        "if ($0) localStorage.setItem('eatgo-theme', $0);",
                        theme
                    );
                    
                } catch (Exception e) {
                    System.err.println("SettingsView: Error parsing settings: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    getUI().ifPresent(u -> {
                        u.getPage().executeJs("setTimeout(function() { $0.$server.setInitializationComplete(); }, 200);", getElement());
                    });
                }
            });
        });
    }
    
    @com.vaadin.flow.component.ClientCallable
    public void onError(String errorMessage) {
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                Notification.show("Błąd: " + errorMessage, 5000, Notification.Position.TOP_CENTER);
            });
        });
    }
    
    @com.vaadin.flow.component.ClientCallable
    public void setInitializationComplete() {
        isInitializing = false;
    }
    
    @com.vaadin.flow.component.ClientCallable
    public void loadUserAddresses(String userIdStr) {
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                try {
                    Long userId = Long.parseLong(userIdStr);
                    userAddresses = addressService.listAddresses(userId);
                    if (defaultAddressComboBox != null) {
                        defaultAddressComboBox.setItems(userAddresses);
                    }
                } catch (Exception e) {
                    System.err.println("SettingsView: Error loading addresses: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        });
    }
    
    
    private Div createPasswordSection() {
        Div section = new Div();
        section.addClassName("settings-section");
        section.getStyle().set("margin-bottom", "2rem");
        section.getStyle().set("padding", "1.5rem");
        section.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        section.getStyle().set("border-radius", "8px");
        
        H3 sectionTitle = new H3("Zmiana hasła");
        sectionTitle.getStyle().set("margin-top", "0");
        
        oldPasswordField = new PasswordField("Obecne hasło");
        oldPasswordField.setWidthFull();
        oldPasswordField.setRequired(true);
        
        newPasswordField = new PasswordField("Nowe hasło");
        newPasswordField.setWidthFull();
        newPasswordField.setRequired(true);
        newPasswordField.setValueChangeMode(ValueChangeMode.EAGER);
        newPasswordField.addValueChangeListener(e -> updatePasswordStrength(e.getValue()));

        passwordStrengthIndicator = new Div();
        passwordStrengthIndicator.addClassName("password-strength-indicator");
        passwordStrengthIndicator.setVisible(false);
        
        passwordStrengthBar = new ProgressBar(0, 4);
        passwordStrengthBar.setWidthFull();
        passwordStrengthBar.addClassName("password-strength-bar");

        passwordRequirements = new Div();
        passwordRequirements.addClassName("password-requirements");
        passwordRequirements.getStyle().set("font-size", "0.875rem");
        passwordRequirements.getStyle().set("margin-top", "0.5rem");
        
        Div requirementsList = new Div();
        requirementsList.addClassName("requirements-list");
        
        Span req1 = new Span("• Co najmniej " + MIN_PASSWORD_LENGTH + " znaków");
        req1.addClassName("requirement-item");
        req1.addClassName("requirement-unmet");
        req1.setId("req-length");
        
        Span req2 = new Span("• Przynajmniej jedną wielką literę");
        req2.addClassName("requirement-item");
        req2.addClassName("requirement-unmet");
        req2.setId("req-upper");
        
        Span req3 = new Span("• Przynajmniej jedną małą literę");
        req3.addClassName("requirement-item");
        req3.addClassName("requirement-unmet");
        req3.setId("req-lower");
        
        Span req4 = new Span("• Przynajmniej jedną cyfrę");
        req4.addClassName("requirement-item");
        req4.addClassName("requirement-unmet");
        req4.setId("req-digit");
        
        Span req5 = new Span("• Przynajmniej jeden znak specjalny (!@#$%^&*)");
        req5.addClassName("requirement-item");
        req5.addClassName("requirement-unmet");
        req5.setId("req-special");
        
        requirementsList.add(req1, req2, req3, req4, req5);
        passwordRequirements.add(requirementsList);
        
        confirmPasswordField = new PasswordField("Potwierdź nowe hasło");
        confirmPasswordField.setWidthFull();
        confirmPasswordField.setRequired(true);
        
        Button changePasswordBtn = new Button("Zmień hasło", VaadinIcon.LOCK.create());
        changePasswordBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        changePasswordBtn.addClickListener(e -> changePassword());
        
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setPadding(false);
        layout.add(sectionTitle, oldPasswordField, newPasswordField, passwordStrengthIndicator, passwordRequirements, confirmPasswordField, changePasswordBtn);
        
        section.add(layout);
        return section;
    }
    
    private Div createDefaultAddressSection() {
        Div section = new Div();
        section.addClassName("settings-section");
        section.getStyle().set("margin-bottom", "2rem");
        section.getStyle().set("padding", "1.5rem");
        section.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        section.getStyle().set("border-radius", "8px");
        
        H3 sectionTitle = new H3("Domyślny adres dostawy");
        sectionTitle.getStyle().set("margin-top", "0");
        
        defaultAddressComboBox = new ComboBox<>("Wybierz domyślny adres dostawy");
        defaultAddressComboBox.setWidthFull();
        defaultAddressComboBox.setItemLabelGenerator(address -> {
            String fullAddress = address.street();
            if (address.apartmentNumber() != null && !address.apartmentNumber().isEmpty()) {
                fullAddress += "/" + address.apartmentNumber();
            }
            fullAddress += ", " + address.postalCode() + " " + address.city();
            return fullAddress;
        });
        defaultAddressComboBox.addValueChangeListener(e -> {
            if (e.getValue() != null && !isInitializing) {
                saveDefaultAddress(e.getValue().id());
            }
        });
        
        Span description = new Span("Ten adres będzie domyślnie wybierany podczas składania zamówienia.");
        description.getStyle().set("color", "var(--lumo-secondary-text-color)");
        description.getStyle().set("font-size", "0.875rem");
        
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setPadding(false);
        layout.add(sectionTitle, defaultAddressComboBox, description);
        
        section.add(layout);
        return section;
    }
    
    private Div createDefaultCitySection() {
        Div section = new Div();
        section.addClassName("settings-section");
        section.getStyle().set("margin-bottom", "2rem");
        section.getStyle().set("padding", "1.5rem");
        section.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        section.getStyle().set("border-radius", "8px");
        
        H3 sectionTitle = new H3("Domyślne miasto");
        sectionTitle.getStyle().set("margin-top", "0");
        
        defaultCityComboBox = new ComboBox<>("Wybierz domyślne miasto");
        defaultCityComboBox.setItems(AVAILABLE_CITIES);
        defaultCityComboBox.setWidthFull();
        defaultCityComboBox.addValueChangeListener(e -> {
            if (e.getValue() != null && !isInitializing) {
                saveDefaultCity(e.getValue());
            }
        });
        
        Span description = new Span("To miasto będzie domyślnie wybierane na stronie głównej.");
        description.getStyle().set("color", "var(--lumo-secondary-text-color)");
        description.getStyle().set("font-size", "0.875rem");
        
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setPadding(false);
        layout.add(sectionTitle, defaultCityComboBox, description);
        
        section.add(layout);
        return section;
    }
    
    private Div createThemeSection() {
        Div section = new Div();
        section.addClassName("settings-section");
        section.getStyle().set("margin-bottom", "2rem");
        section.getStyle().set("padding", "1.5rem");
        section.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        section.getStyle().set("border-radius", "8px");
        
        H3 sectionTitle = new H3("Motyw strony");
        sectionTitle.getStyle().set("margin-top", "0");
        
        themeRadioGroup = new RadioButtonGroup<>();
        themeRadioGroup.setItems("light", "dark");
        themeRadioGroup.setLabel("Wybierz motyw");
        themeRadioGroup.setItemLabelGenerator(item -> item.equals("light") ? "Jasny" : "Ciemny");
        themeRadioGroup.addValueChangeListener(e -> {
            if (e.getValue() != null && !isInitializing) {
                saveTheme(e.getValue());
            }
        });
        
        Span description = new Span("Wybierz preferowany motyw wyświetlania strony.");
        description.getStyle().set("color", "var(--lumo-secondary-text-color)");
        description.getStyle().set("font-size", "0.875rem");
        
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setPadding(false);
        layout.add(sectionTitle, themeRadioGroup, description);
        
        section.add(layout);
        return section;
    }
    
    private void changePassword() {
        String oldPassword = oldPasswordField.getValue();
        String newPassword = newPasswordField.getValue();
        String confirmPassword = confirmPasswordField.getValue();

        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            Notification.show("Wprowadź obecne hasło", 3000, Notification.Position.TOP_CENTER);
            oldPasswordField.focus();
            return;
        }
        
        if (newPassword == null || newPassword.trim().isEmpty()) {
            Notification.show("Wprowadź nowe hasło", 3000, Notification.Position.TOP_CENTER);
            newPasswordField.focus();
            return;
        }

        int strength = calculatePasswordStrength(newPassword);
        if (strength < 2) {
            Notification.show("Hasło jest zbyt słabe. Upewnij się, że spełnia wszystkie wymagania.", 5000, Notification.Position.TOP_CENTER);
            newPasswordField.focus();
            return;
        }
        
        if (newPassword.length() < MIN_PASSWORD_LENGTH) {
            Notification.show("Nowe hasło musi mieć co najmniej " + MIN_PASSWORD_LENGTH + " znaków", 3000, Notification.Position.TOP_CENTER);
            newPasswordField.focus();
            return;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            Notification.show("Nowe hasła nie są identyczne", 3000, Notification.Position.TOP_CENTER);
            confirmPasswordField.focus();
            return;
        }

        getElement().executeJs(
            "const token = localStorage.getItem('eatgo-token'); " +
            "if (!token) { " +
            "  $0.$server.onPasswordChangeError('Musisz być zalogowany'); " +
            "  return; " +
            "} " +
            "fetch('/api/auth/password', { " +
            "  method: 'PUT', " +
            "  headers: { " +
            "    'Authorization': 'Bearer ' + token, " +
            "    'Content-Type': 'application/json' " +
            "  }, " +
            "  body: JSON.stringify({ " +
            "    oldPassword: $1, " +
            "    newPassword: $2 " +
            "  }) " +
            "}) " +
            ".then(r => { " +
            "  if (!r.ok) { " +
            "    return r.text().then(text => { " +
            "      throw new Error(text || 'Błąd podczas zmiany hasła'); " +
            "    }); " +
            "  } " +
            "  return r.text(); " +
            "}) " +
            ".then(message => { " +
            "  console.log('Password changed successfully:', message); " +
            "  $0.$server.onPasswordChangeSuccess(); " +
            "}) " +
            ".catch(e => { " +
            "  console.error('Error changing password:', e); " +
            "  $0.$server.onPasswordChangeError(e.message || 'Błąd podczas zmiany hasła'); " +
            "});",
            getElement(), oldPassword, newPassword
        );
    }
    
    @com.vaadin.flow.component.ClientCallable
    public void onPasswordChangeSuccess() {
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                Notification.show("Hasło zostało zmienione pomyślnie!", 3000, Notification.Position.TOP_CENTER);
                oldPasswordField.clear();
                newPasswordField.clear();
                confirmPasswordField.clear();
            });
        });
    }
    
    @com.vaadin.flow.component.ClientCallable
    public void onPasswordChangeError(String errorMessage) {
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                Notification.show("Błąd: " + errorMessage, 5000, Notification.Position.TOP_CENTER);
            });
        });
    }
    
    private void saveDefaultAddress(Long addressId) {
        saveSettings(null, addressId, null);
    }
    
    private void saveDefaultCity(String city) {
        saveSettings(city, null, null);
    }
    
    private void saveTheme(String theme) {
        saveSettings(null, null, theme);
    }
    
    private void saveSettings(String city, Long addressId, String theme) {
        String finalCity = city != null ? city : (defaultCityComboBox.getValue() != null ? defaultCityComboBox.getValue() : null);
        Long finalAddressId = addressId != null ? addressId : (defaultAddressComboBox.getValue() != null ? defaultAddressComboBox.getValue().id() : null);
        String finalTheme = theme != null ? theme : (themeRadioGroup.getValue() != null ? themeRadioGroup.getValue() : "light");
        
        getElement().executeJs(
            "const token = localStorage.getItem('eatgo-token'); " +
            "if (!token) { " +
            "  $0.$server.onError('Musisz być zalogowany'); " +
            "  return; " +
            "} " +
            "const settings = { " +
            "  defaultCity: $1, " +
            "  defaultAddressId: $2, " +
            "  theme: $3 " +
            "}; " +
            "fetch('/api/users/settings', { " +
            "  method: 'PUT', " +
            "  headers: { " +
            "    'Authorization': 'Bearer ' + token, " +
            "    'Content-Type': 'application/json' " +
            "  }, " +
            "  body: JSON.stringify(settings) " +
            "}) " +
            ".then(r => { " +
            "  if (!r.ok) { " +
            "    return r.text().then(text => { " +
            "      throw new Error(text || 'Błąd podczas zapisywania ustawień'); " +
            "    }); " +
            "  } " +
            "  return r.json(); " +
            "}) " +
            ".then(settings => { " +
            "  console.log('Settings saved:', settings); " +
            "  if ($4) { " +
            "    // Zaktualizuj motyw w localStorage i DOM " +
            "    localStorage.setItem('eatgo-theme', settings.theme); " +
            "    document.documentElement.setAttribute('data-theme', settings.theme); " +
            "    $0.$server.onSettingsSaved(true); " +
            "  } else { " +
            "    $0.$server.onSettingsSaved(false); " +
            "  } " +
            "}) " +
            ".catch(e => { " +
            "  console.error('Error saving settings:', e); " +
            "  $0.$server.onError('Błąd podczas zapisywania ustawień: ' + e.message); " +
            "});",
            getElement(),
            finalCity != null ? finalCity : "null",
            finalAddressId != null ? String.valueOf(finalAddressId) : "null",
            finalTheme,
            theme != null // czy to zmiana motywu
        );
    }
    
    @com.vaadin.flow.component.ClientCallable
    public void onSettingsSaved(boolean isThemeChange) {
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                if (!isInitializing) {
                    if (isThemeChange) {
                        Notification.show("Motyw został zapisany", 2000, Notification.Position.TOP_CENTER);
                        ui.getPage().executeJs("setTimeout(function() { window.location.reload(); }, 500);");
                    } else {
                        Notification.show("Ustawienia zostały zapisane", 2000, Notification.Position.TOP_CENTER);
                    }
                }
            });
        });
    }
    
    private void updatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            passwordStrengthIndicator.setVisible(false);
            passwordRequirements.getStyle().set("opacity", "0.5");
            return;
        }
        
        passwordStrengthIndicator.setVisible(true);
        passwordRequirements.getStyle().set("opacity", "1");
        
        int strength = calculatePasswordStrength(password);
        passwordStrengthBar.setValue(strength);

        String strengthText;
        String strengthClass;
        
        if (strength <= 1) {
            strengthText = "Bardzo słabe";
            strengthClass = "strength-weak";
        } else if (strength == 2) {
            strengthText = "Słabe";
            strengthClass = "strength-fair";
        } else if (strength == 3) {
            strengthText = "Dobre";
            strengthClass = "strength-good";
        } else {
            strengthText = "Bardzo silne";
            strengthClass = "strength-strong";
        }
        
        passwordStrengthIndicator.removeAll();
        passwordStrengthIndicator.addClassName("password-strength");
        passwordStrengthIndicator.removeClassName("strength-weak");
        passwordStrengthIndicator.removeClassName("strength-fair");
        passwordStrengthIndicator.removeClassName("strength-good");
        passwordStrengthIndicator.removeClassName("strength-strong");
        passwordStrengthIndicator.addClassName(strengthClass);
        
        Span strengthLabel = new Span("Siła hasła: " + strengthText);
        strengthLabel.addClassName("strength-label");
        passwordStrengthIndicator.add(strengthLabel, passwordStrengthBar);

        updatePasswordRequirements(password);
    }
    
    private int calculatePasswordStrength(String password) {
        int strength = 0;

        if (password.length() >= MIN_PASSWORD_LENGTH) strength++;
        if (password.length() >= 12) strength++;

        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        boolean hasSpecial = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");
        
        if (hasUpper && hasLower) strength++;
        if (hasDigit) strength++;
        if (hasSpecial) strength++;
        
        return Math.min(strength, 4);
    }
    
    private void updatePasswordRequirements(String password) {
        if (password == null || password.isEmpty()) {
            updateRequirementStatus("req-length", false);
            updateRequirementStatus("req-upper", false);
            updateRequirementStatus("req-lower", false);
            updateRequirementStatus("req-digit", false);
            updateRequirementStatus("req-special", false);
            return;
        }
        
        boolean[] checks = {
            password.length() >= MIN_PASSWORD_LENGTH,
            password.matches(".*[A-Z].*"),
            password.matches(".*[a-z].*"),
            password.matches(".*[0-9].*"),
            password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")
        };
        
        String[] reqIds = {"req-length", "req-upper", "req-lower", "req-digit", "req-special"};
        
        for (int i = 0; i < reqIds.length && i < checks.length; i++) {
            updateRequirementStatus(reqIds[i], checks[i]);
        }
    }
    
    private void updateRequirementStatus(String reqId, boolean met) {
        passwordRequirements.getChildren()
            .filter(child -> child instanceof Div)
            .map(child -> (Div) child)
            .flatMap(div -> div.getChildren())
            .filter(child -> child instanceof Span)
            .map(child -> (Span) child)
            .filter(span -> reqId.equals(span.getId().orElse("")))
            .findFirst()
            .ifPresent(span -> {
                if (met) {
                    span.removeClassName("requirement-unmet");
                    span.addClassName("requirement-met");
                    span.getStyle().set("color", "var(--lumo-success-color)");
                } else {
                    span.removeClassName("requirement-met");
                    span.addClassName("requirement-unmet");
                    span.getStyle().set("color", "var(--lumo-error-color)");
                }
            });
    }
}

