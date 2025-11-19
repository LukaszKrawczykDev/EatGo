package pollub.eatgo.views.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import pollub.eatgo.service.AuthenticationService;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RegisterDialog extends Dialog {
    
    private final AuthenticationService authService;
    private EmailField emailField;
    private PasswordField passwordField;
    private PasswordField confirmPasswordField;
    private TextField fullNameField;
    private RadioButtonGroup<String> roleRadioGroup;
    private Div restaurantFieldsContainer;
    private TextField restaurantNameField;
    private TextField restaurantAddressField;
    private NumberField restaurantDeliveryPriceField;
    private Button registerButton;
    private Div passwordStrengthIndicator;
    private ProgressBar passwordStrengthBar;
    private Div passwordRequirements;
    
    private static final List<String> AVAILABLE_ROLES = Arrays.asList("CLIENT", "RESTAURANT_ADMIN");
    
    // Wymagania hasła
    private static final int MIN_PASSWORD_LENGTH = 8;
    
    public RegisterDialog(AuthenticationService authService) {
        this.authService = authService;
        
        addClassName("auth-dialog");
        setHeaderTitle("Zarejestruj się");
        setWidth("500px");
        setMaxWidth("90vw");
        setMaxHeight("90vh");
        
        VerticalLayout formLayout = new VerticalLayout();
        formLayout.addClassName("auth-form");
        formLayout.setSpacing(true);
        formLayout.setPadding(true);
        formLayout.setAlignItems(Alignment.CENTER);
        formLayout.getStyle().set("overflow-y", "auto");
        
        emailField = new EmailField("Email");
        emailField.setWidthFull();
        emailField.setRequired(true);
        emailField.setErrorMessage("Podaj prawidłowy adres email");
        emailField.addClassName("auth-input");
        
        passwordField = new PasswordField("Hasło");
        passwordField.setWidthFull();
        passwordField.setRequired(true);
        passwordField.setRequiredIndicatorVisible(true);
        passwordField.setValueChangeMode(ValueChangeMode.EAGER);
        passwordField.addClassName("auth-input");
        passwordField.addValueChangeListener(e -> updatePasswordStrength(e.getValue()));
        
        // Wskaźnik siły hasła
        passwordStrengthIndicator = new Div();
        passwordStrengthIndicator.addClassName("password-strength-indicator");
        passwordStrengthIndicator.setVisible(false);
        
        passwordStrengthBar = new ProgressBar();
        passwordStrengthBar.setMin(0);
        passwordStrengthBar.setMax(4);
        passwordStrengthBar.setValue(0);
        passwordStrengthBar.addClassName("password-strength-bar");
        
        passwordRequirements = new Div();
        passwordRequirements.addClassName("password-requirements");
        createPasswordRequirements();
        
        confirmPasswordField = new PasswordField("Potwierdź hasło");
        confirmPasswordField.setWidthFull();
        confirmPasswordField.setRequired(true);
        confirmPasswordField.setRequiredIndicatorVisible(true);
        confirmPasswordField.setValueChangeMode(ValueChangeMode.EAGER);
        confirmPasswordField.addClassName("auth-input");
        confirmPasswordField.addValueChangeListener(e -> validatePasswordMatch());
        passwordField.addValueChangeListener(e -> validatePasswordMatch()); // Również przy zmianie głównego hasła
        
        fullNameField = new TextField("Imię i nazwisko");
        fullNameField.setWidthFull();
        fullNameField.setRequired(true);
        fullNameField.addClassName("auth-input");
        
        roleRadioGroup = new RadioButtonGroup<>();
        roleRadioGroup.setLabel("Wybierz typ konta");
        roleRadioGroup.setItems("CLIENT", "RESTAURANT_ADMIN");
        roleRadioGroup.setRequiredIndicatorVisible(true);
        roleRadioGroup.setWidthFull();
        roleRadioGroup.addClassName("auth-input");
        roleRadioGroup.setItemLabelGenerator(item -> {
            if ("CLIENT".equals(item)) return "Klient";
            if ("RESTAURANT_ADMIN".equals(item)) return "Właściciel restauracji";
            return item;
        });
        roleRadioGroup.addValueChangeListener(e -> {
            String value = e.getValue();
            if (value != null) {
                toggleRestaurantFields(value);
            }
        });
        
        restaurantFieldsContainer = new Div();
        restaurantFieldsContainer.addClassName("restaurant-fields-container");
        restaurantFieldsContainer.setVisible(false);
        
        restaurantNameField = new TextField("Nazwa restauracji");
        restaurantNameField.setWidthFull();
        restaurantNameField.setRequired(true);
        restaurantNameField.addClassName("auth-input");
        
        restaurantAddressField = new TextField("Adres restauracji");
        restaurantAddressField.setWidthFull();
        restaurantAddressField.setRequired(true);
        restaurantAddressField.addClassName("auth-input");
        
        restaurantDeliveryPriceField = new NumberField("Cena dostawy (zł)");
        restaurantDeliveryPriceField.setWidthFull();
        restaurantDeliveryPriceField.setMin(0);
        restaurantDeliveryPriceField.setValue(0.0);
        restaurantDeliveryPriceField.addClassName("auth-input");
        
        restaurantFieldsContainer.add(restaurantNameField, restaurantAddressField, restaurantDeliveryPriceField);
        
        registerButton = new Button("Zarejestruj się");
        registerButton.addClassName("auth-submit-btn");
        registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        registerButton.setWidthFull();
        registerButton.addClickListener(e -> handleRegister());
        
        Paragraph loginLink = new Paragraph();
        loginLink.addClassName("auth-link-text");
        loginLink.setText("Masz już konto? ");
        Button loginBtn = new Button("Zaloguj się");
        loginBtn.addClassName("auth-link");
        loginBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        loginBtn.addClickListener(e -> {
            close();
            if (onLoginCallback != null) {
                onLoginCallback.run();
            }
        });
        loginLink.add(loginBtn);
        
        formLayout.add(emailField, passwordField, passwordStrengthIndicator, passwordRequirements, 
                      confirmPasswordField, fullNameField, 
                      roleRadioGroup, restaurantFieldsContainer, registerButton, loginLink);
        add(formLayout);
        
        Button closeBtn = new Button("Anuluj", e -> close());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        getFooter().add(closeBtn);
    }
    
    private Runnable onLoginCallback;
    
    public void setOnLoginClick(Runnable onLogin) {
        this.onLoginCallback = onLogin;
    }
    
    private void createPasswordRequirements() {
        passwordRequirements.removeAll();
        
        Span title = new Span("Hasło musi zawierać:");
        title.addClassName("requirements-title");
        passwordRequirements.add(title);
        
        Div requirementsList = new Div();
        requirementsList.addClassName("requirements-list");
        
        // Tworzymy wymagania jako osobne elementy, które będziemy mogli łatwo aktualizować
        Span req1 = new Span("• Minimum " + MIN_PASSWORD_LENGTH + " znaków");
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
        
        // Aktualizuj kolor i tekst
        String strengthText;
        String strengthClass;
        double percentage = (strength / 4.0) * 100;
        
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
        
        // Aktualizuj wymagania
        updatePasswordRequirements(password);
    }
    
    private int calculatePasswordStrength(String password) {
        int strength = 0;
        
        // Długość
        if (password.length() >= MIN_PASSWORD_LENGTH) strength++;
        if (password.length() >= 12) strength++;
        
        // Różnorodność znaków
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
            // Reset wszystkich wymagań
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
            .filter(span -> span instanceof Span && span.getId().isPresent() && reqId.equals(span.getId().get()))
            .findFirst()
            .ifPresent(span -> {
                span.removeClassName("requirement-met");
                span.removeClassName("requirement-unmet");
                span.addClassName(met ? "requirement-met" : "requirement-unmet");
            });
    }
    
    private void validatePasswordMatch() {
        String password = passwordField.getValue();
        String confirm = confirmPasswordField.getValue();
        
        if (confirm != null && !confirm.isEmpty()) {
            if (!password.equals(confirm)) {
                confirmPasswordField.setErrorMessage("Hasła nie są identyczne");
                confirmPasswordField.setInvalid(true);
            } else {
                confirmPasswordField.setErrorMessage(null);
                confirmPasswordField.setInvalid(false);
            }
        }
    }
    
    private void toggleRestaurantFields(String role) {
        boolean isRestaurantAdmin = "RESTAURANT_ADMIN".equals(role);
        restaurantFieldsContainer.setVisible(isRestaurantAdmin);
        
        if (isRestaurantAdmin) {
            restaurantNameField.setRequired(true);
            restaurantAddressField.setRequired(true);
        } else {
            restaurantNameField.setRequired(false);
            restaurantAddressField.setRequired(false);
        }
    }
    
    private void handleRegister() {
        // Walidacja podstawowych pól
        if (emailField.getValue() == null || emailField.getValue().isBlank()) {
            Notification.show("Podaj adres email", 3000, Notification.Position.TOP_CENTER);
            return;
        }
        
        String password = passwordField.getValue();
        if (password == null || password.isBlank()) {
            Notification.show("Podaj hasło", 3000, Notification.Position.TOP_CENTER);
            passwordField.setInvalid(true);
            return;
        }
        
        // Walidacja siły hasła
        String passwordError = validatePassword(password);
        if (passwordError != null) {
            Notification.show(passwordError, 4000, Notification.Position.TOP_CENTER);
            passwordField.setInvalid(true);
            passwordField.setErrorMessage(passwordError);
            return;
        }
        
        if (!password.equals(confirmPasswordField.getValue())) {
            Notification.show("Hasła nie są identyczne", 3000, Notification.Position.TOP_CENTER);
            confirmPasswordField.setInvalid(true);
            return;
        }
        
        if (fullNameField.getValue() == null || fullNameField.getValue().isBlank()) {
            Notification.show("Podaj imię i nazwisko", 3000, Notification.Position.TOP_CENTER);
            return;
        }
        
        if (roleRadioGroup.getValue() == null || roleRadioGroup.getValue().isBlank()) {
            Notification.show("Wybierz typ konta", 3000, Notification.Position.TOP_CENTER);
            return;
        }
        
        // Walidacja pól restauracji dla RESTAURANT_ADMIN
        if ("RESTAURANT_ADMIN".equals(roleRadioGroup.getValue())) {
            if (restaurantNameField.getValue() == null || restaurantNameField.getValue().isBlank()) {
                Notification.show("Podaj nazwę restauracji", 3000, Notification.Position.TOP_CENTER);
                return;
            }
            
            if (restaurantAddressField.getValue() == null || restaurantAddressField.getValue().isBlank()) {
                Notification.show("Podaj adres restauracji", 3000, Notification.Position.TOP_CENTER);
                return;
            }
        }
        
        registerButton.setEnabled(false);
        registerButton.setText("Rejestracja...");
        
        // Wykonaj rejestrację synchronicznie
        try {
            String role = roleRadioGroup.getValue();
            String restaurantName = "RESTAURANT_ADMIN".equals(role) ? restaurantNameField.getValue() : null;
            String restaurantAddress = "RESTAURANT_ADMIN".equals(role) ? restaurantAddressField.getValue() : null;
            Double deliveryPrice = "RESTAURANT_ADMIN".equals(role) && restaurantDeliveryPriceField.getValue() != null 
                    ? restaurantDeliveryPriceField.getValue() : null;
            
            AuthenticationService.AuthResult result = authService.register(
                    emailField.getValue(),
                    passwordField.getValue(),
                    fullNameField.getValue(),
                    role,
                    restaurantName,
                    restaurantAddress,
                    deliveryPrice
            );
            
            registerButton.setEnabled(true);
            registerButton.setText("Zarejestruj się");
            
            if (result.isSuccess()) {
                // Zapisz token w localStorage
                saveTokenToLocalStorage(result.getToken(), result.getUserId(), result.getRole());
                
                close();
                
                // Wyślij event natychmiast po zamknięciu dialogu
                // Użyj setTimeout, aby dać czas na zamknięcie dialogu
                getUI().ifPresent(ui -> {
                    ui.getPage().executeJs(
                        "setTimeout(function() { " +
                        "  console.log('Dispatching eatgo-login-changed event'); " +
                        "  window.dispatchEvent(new CustomEvent('eatgo-login-changed')); " +
                        "}, 200);"
                    );
                });
                
                Notification.show("Rejestracja zakończona pomyślnie!", 2000, Notification.Position.TOP_CENTER);
            } else {
                Notification.show(result.getErrorMessage(), 4000, Notification.Position.TOP_CENTER);
            }
        } catch (Exception e) {
            registerButton.setEnabled(true);
            registerButton.setText("Zarejestruj się");
            Notification.show("Wystąpił błąd podczas rejestracji: " + e.getMessage(), 4000, Notification.Position.TOP_CENTER);
        }
    }
    
    private String validatePassword(String password) {
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return "Hasło musi mieć minimum " + MIN_PASSWORD_LENGTH + " znaków";
        }
        
        if (!password.matches(".*[A-Z].*")) {
            return "Hasło musi zawierać przynajmniej jedną wielką literę";
        }
        
        if (!password.matches(".*[a-z].*")) {
            return "Hasło musi zawierać przynajmniej jedną małą literę";
        }
        
        if (!password.matches(".*[0-9].*")) {
            return "Hasło musi zawierać przynajmniej jedną cyfrę";
        }
        
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            return "Hasło musi zawierać przynajmniej jeden znak specjalny (!@#$%^&*)";
        }
        
        return null; // Hasło jest poprawne
    }
    
    private void saveTokenToLocalStorage(String token, Long userId, String role) {
        getElement().executeJs(
            "localStorage.setItem('eatgo-token', $0); " +
            "localStorage.setItem('eatgo-userId', $1); " +
            "localStorage.setItem('eatgo-role', $2);",
            token, userId != null ? userId.toString() : "", role != null ? role : ""
        );
    }
}

