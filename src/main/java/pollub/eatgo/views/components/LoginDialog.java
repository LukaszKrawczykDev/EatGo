package pollub.eatgo.views.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import pollub.eatgo.service.AuthenticationService;

public class LoginDialog extends Dialog {
    
    private final AuthenticationService authService;
    private EmailField emailField;
    private PasswordField passwordField;
    private Button loginButton;
    private Runnable onRegisterCallback;
    
    public LoginDialog(AuthenticationService authService) {
        this.authService = authService;
        
        addClassName("auth-dialog");
        setHeaderTitle("Zaloguj się");
        setWidth("500px");
        setMaxWidth("90vw");
        
        VerticalLayout formLayout = new VerticalLayout();
        formLayout.addClassName("auth-form");
        formLayout.setSpacing(true);
        formLayout.setPadding(true);
        formLayout.setAlignItems(Alignment.CENTER);
        
        emailField = new EmailField("Email");
        emailField.setWidthFull();
        emailField.setRequired(true);
        emailField.setErrorMessage("Podaj prawidłowy adres email");
        emailField.addClassName("auth-input");
        
        passwordField = new PasswordField("Hasło");
        passwordField.setWidthFull();
        passwordField.setRequired(true);
        passwordField.addClassName("auth-input");
        
        loginButton = new Button("Zaloguj się");
        loginButton.addClassName("auth-submit-btn");
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.setWidthFull();
        loginButton.addClickListener(e -> handleLogin());
        
        Paragraph registerLink = new Paragraph();
        registerLink.addClassName("auth-link-text");
        registerLink.setText("Nie masz konta? ");
        Button registerBtn = new Button("Zarejestruj się");
        registerBtn.addClassName("auth-link");
        registerBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        registerBtn.addClickListener(e -> {
            close();
            if (onRegisterCallback != null) {
                onRegisterCallback.run();
            }
        });
        registerLink.add(registerBtn);
        
        formLayout.add(emailField, passwordField, loginButton, registerLink);
        add(formLayout);
        
        Button closeBtn = new Button("Anuluj", e -> close());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        getFooter().add(closeBtn);
    }
    
    public void setOnRegisterClick(Runnable onRegister) {
        this.onRegisterCallback = onRegister;
    }
    
    private void handleLogin() {
        if (emailField.getValue() == null || emailField.getValue().isBlank()) {
            Notification.show("Podaj adres email", 3000, Notification.Position.TOP_CENTER);
            return;
        }
        
        if (passwordField.getValue() == null || passwordField.getValue().isBlank()) {
            Notification.show("Podaj hasło", 3000, Notification.Position.TOP_CENTER);
            return;
        }
        
        loginButton.setEnabled(false);
        loginButton.setText("Logowanie...");
        
        // Wykonaj logowanie synchronicznie (szybka operacja)
        try {
            AuthenticationService.AuthResult result = authService.login(
                    emailField.getValue(),
                    passwordField.getValue()
            );
            
            loginButton.setEnabled(true);
            loginButton.setText("Zaloguj się");
            
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
                
                Notification.show("Zalogowano pomyślnie!", 2000, Notification.Position.TOP_CENTER);
            } else {
                Notification.show(result.getErrorMessage(), 4000, Notification.Position.TOP_CENTER);
            }
        } catch (Exception e) {
            loginButton.setEnabled(true);
            loginButton.setText("Zaloguj się");
            Notification.show("Wystąpił błąd podczas logowania. Spróbuj ponownie.", 4000, Notification.Position.TOP_CENTER);
        }
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

