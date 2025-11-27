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
    private final HeaderComponent headerComponent;
    private EmailField emailField;
    private PasswordField passwordField;
    private Button loginButton;
    private Runnable onRegisterCallback;
    
    public LoginDialog(AuthenticationService authService, HeaderComponent headerComponent) {
        this.authService = authService;
        this.headerComponent = headerComponent;
        
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
            RegisterDialog registerDialog = new RegisterDialog(authService, headerComponent);
            registerDialog.open();
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
        
        try {
            AuthenticationService.AuthResult result = authService.login(
                    emailField.getValue(),
                    passwordField.getValue()
            );
            
            loginButton.setEnabled(true);
            loginButton.setText("Zaloguj się");
            
            if (result.isSuccess()) {
                String token = result.getToken();
                String userId = result.getUserId() != null ? result.getUserId().toString() : "";
                String role = result.getRole() != null ? result.getRole() : "";
                
                System.out.println("Login successful - token: " + (token != null ? "present" : "null") + 
                                   ", userId: " + userId + ", role: " + role);
                
                getUI().ifPresent(ui -> {
                    ui.access(() -> {
                        ui.getPage().executeJs(
                            "console.log('Saving token to localStorage...'); " +
                            "localStorage.setItem('eatgo-token', $0); " +
                            "localStorage.setItem('eatgo-userId', $1); " +
                            "localStorage.setItem('eatgo-role', $2); " +
                            "console.log('Token saved! eatgo-token:', localStorage.getItem('eatgo-token')); " +
                            "console.log('UserId saved! eatgo-userId:', localStorage.getItem('eatgo-userId')); " +
                            "console.log('Role saved! eatgo-role:', localStorage.getItem('eatgo-role')); " +
                            "// Wywołaj callback po zapisaniu " +
                            "$3.$server.onTokenSaved($1, $2);",
                            token != null ? token : "", userId, role, headerComponent.getElement()
                        );
                        
                        if ("RESTAURANT_ADMIN".equals(role)) {
                            ui.navigate("restaurant");
                        } else if ("COURIER".equals(role)) {
                            ui.navigate("courier");
                        } else {
                            ui.getPage().reload();
                        }
                    });
                });
                
                Notification.show("Zalogowano pomyślnie!", 2000, Notification.Position.TOP_CENTER);
                close();
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
            "localStorage.setItem('eatgo-role', $2); " +
            "console.log('Token saved to localStorage');",
            token, userId != null ? userId.toString() : "", role != null ? role : ""
        );
    }
}

