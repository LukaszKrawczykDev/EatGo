package pollub.eatgo.views.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.router.RouterLink;
import pollub.eatgo.service.AuthenticationService;

public class HeaderComponent extends Div {
    
    private Button themeToggle;
    private boolean isDarkMode = false;
    private final AuthenticationService authService;
    private HorizontalLayout headerActions;
    private Div userMenuContainer;
    private Div loginButtonsContainer;
    
    public HeaderComponent(AuthenticationService authService) {
        this.authService = authService;
        addClassName("home-header");
        setWidthFull();
        
        Div headerContent = new Div();
        headerContent.addClassName("header-content");
        headerContent.setWidthFull();
        
        Div logoContainer = new Div();
        logoContainer.addClassName("logo-container");
        RouterLink logoLink = new RouterLink("", pollub.eatgo.views.HomeView.class);
        logoLink.addClassName("logo-link");
        H1 logo = new H1("🍔 EatGo");
        logo.addClassName("logo");
        logoLink.add(logo);
        logoContainer.add(logoLink);
        
        headerActions = new HorizontalLayout();
        headerActions.addClassName("header-actions");
        headerActions.setSpacing(true);
        headerActions.setAlignItems(Alignment.CENTER);
        
        themeToggle = new Button();
        themeToggle.addClassName("theme-toggle");
        themeToggle.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        themeToggle.addClickListener(e -> toggleTheme());
        initializeTheme();
        updateThemeToggleIcon();
        
        // Kontenery dla menu użytkownika i przycisków logowania
        userMenuContainer = new Div();
        userMenuContainer.addClassName("user-menu-container");
        userMenuContainer.setVisible(false);
        
        loginButtonsContainer = new Div();
        loginButtonsContainer.addClassName("login-buttons-container");
        createLoginButtons();
        
        headerActions.add(themeToggle, userMenuContainer, loginButtonsContainer);
        headerContent.add(logoContainer, headerActions);
        add(headerContent);
        
        // Sprawdź stan logowania i zaktualizuj UI
        checkLoginStatus();
        
        // Nasłuchuj zmian w localStorage (dla aktualizacji po zalogowaniu)
        setupStorageListener();
    }
    
    private void setupStorageListener() {
        getElement().executeJs(
            "window.addEventListener('storage', function(e) { " +
            "  if (e.key === 'eatgo-token') { " +
            "    console.log('Storage event detected for eatgo-token'); " +
            "    $0.$server.onLoginStatusChanged(); " +
            "  } " +
            "}); " +
            "// Nasłuchuj również custom event dla zmian w localStorage w tej samej karcie " +
            "window.addEventListener('eatgo-login-changed', function() { " +
            "  console.log('eatgo-login-changed event received'); " +
            "  $0.$server.onLoginStatusChanged(); " +
            "}); " +
            "console.log('Storage listener setup complete');",
            getElement()
        );
    }
    
    @com.vaadin.flow.component.ClientCallable
    private void onLoginStatusChanged() {
        // Użyj UI.access, aby upewnić się, że aktualizacja jest w odpowiednim wątku
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                checkLoginStatus();
            });
        });
    }
    
    private void createLoginButtons() {
        loginButtonsContainer.removeAll();
        
        Button loginBtn = new Button("Zaloguj się");
        loginBtn.addClassName("login-btn");
        loginBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        loginBtn.addClickListener(e -> {
            LoginDialog loginDialog = new LoginDialog(authService);
            RegisterDialog registerDialog = new RegisterDialog(authService);
            
            loginDialog.setOnRegisterClick(() -> {
                loginDialog.close();
                registerDialog.open();
            });
            registerDialog.setOnLoginClick(() -> {
                registerDialog.close();
                loginDialog.open();
            });
            
            loginDialog.open();
        });
        
        Button signUpBtn = new Button("Zarejestruj się");
        signUpBtn.addClassName("signup-btn");
        signUpBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        signUpBtn.addClickListener(e -> {
            LoginDialog loginDialog = new LoginDialog(authService);
            RegisterDialog registerDialog = new RegisterDialog(authService);
            
            loginDialog.setOnRegisterClick(() -> {
                loginDialog.close();
                registerDialog.open();
            });
            registerDialog.setOnLoginClick(() -> {
                registerDialog.close();
                loginDialog.open();
            });
            
            registerDialog.open();
        });
        
        loginButtonsContainer.add(loginBtn, signUpBtn);
    }
    
    private void checkLoginStatus() {
        getElement().executeJs(
            "const token = localStorage.getItem('eatgo-token'); " +
            "const userId = localStorage.getItem('eatgo-userId'); " +
            "const role = localStorage.getItem('eatgo-role'); " +
            "if (token && token !== 'null' && token !== '') { " +
            "  $0.$server.onLoginStatusReceived(true, userId || '', role || ''); " +
            "} else { " +
            "  $0.$server.onLoginStatusReceived(false, '', ''); " +
            "}",
            getElement()
        );
    }
    
    @com.vaadin.flow.component.ClientCallable
    private void onLoginStatusReceived(boolean isLoggedIn, String userId, String role) {
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                if (isLoggedIn) {
                    createUserMenu(userId, role);
                    userMenuContainer.setVisible(true);
                    loginButtonsContainer.setVisible(false);
                } else {
                    userMenuContainer.setVisible(false);
                    loginButtonsContainer.setVisible(true);
                }
            });
        });
    }
    
    private void createUserMenu(String userId, String role) {
        userMenuContainer.removeAll();
        
        MenuBar userMenu = new MenuBar();
        userMenu.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);
        userMenu.addClassName("user-menu");
        
        // Dodaj menu items
        var profileItem = userMenu.addItem("Profil", e -> {
            Notification.show("Profil - w budowie", 2000, Notification.Position.TOP_CENTER);
        });
        profileItem.addComponentAsFirst(VaadinIcon.USER.create());
        
        var ordersItem = userMenu.addItem("Zamówienia", e -> {
            Notification.show("Zamówienia - w budowie", 2000, Notification.Position.TOP_CENTER);
        });
        ordersItem.addComponentAsFirst(VaadinIcon.LIST.create());
        
        var notificationsItem = userMenu.addItem("Powiadomienia", e -> {
            Notification.show("Powiadomienia - w budowie", 2000, Notification.Position.TOP_CENTER);
        });
        notificationsItem.addComponentAsFirst(VaadinIcon.BELL.create());
        
        var settingsItem = userMenu.addItem("Ustawienia", e -> {
            Notification.show("Ustawienia - w budowie", 2000, Notification.Position.TOP_CENTER);
        });
        settingsItem.addComponentAsFirst(VaadinIcon.COG.create());
        
        var logoutItem = userMenu.addItem("Wyloguj się", e -> handleLogout());
        logoutItem.addComponentAsFirst(VaadinIcon.SIGN_OUT.create());
        
        // Dodaj przycisk koszyków (wiele koszyków - jeden per restauracja)
        Button cartsButton = new Button(VaadinIcon.CART.create());
        cartsButton.addClassName("cart-button");
        cartsButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        cartsButton.setTooltipText("Koszyki");
        cartsButton.addClickListener(e -> showCartsDialog());
        
        // Dodaj badge z liczbą aktywnych koszyków
        Span cartsBadge = new Span("0");
        cartsBadge.addClassName("cart-badge");
        updateCartsBadge(cartsBadge);
        
        Div cartContainer = new Div();
        cartContainer.addClassName("cart-container");
        cartContainer.add(cartsButton, cartsBadge);
        
        userMenuContainer.add(cartContainer, userMenu);
    }
    
    private void updateCartsBadge(Span badge) {
        getElement().executeJs(
            "const carts = localStorage.getItem('eatgo-carts'); " +
            "if (carts) { " +
            "  try { " +
            "    const cartsObj = JSON.parse(carts); " +
            "    const count = Object.keys(cartsObj).length; " +
            "    return count; " +
            "  } catch(e) { " +
            "    return 0; " +
            "  } " +
            "} " +
            "return 0;"
        ).then(Integer.class, count -> {
            if (count != null && count > 0) {
                badge.setText(String.valueOf(count));
                badge.setVisible(true);
            } else {
                badge.setVisible(false);
            }
        });
    }
    
    private void showCartsDialog() {
        getElement().executeJs(
            "const carts = localStorage.getItem('eatgo-carts'); " +
            "if (carts) { " +
            "  try { " +
            "    return JSON.parse(carts); " +
            "  } catch(e) { " +
            "    return {}; " +
            "  } " +
            "} " +
            "return {};"
        ).then(String.class, cartsJson -> {
            if (cartsJson != null && !cartsJson.isEmpty() && !cartsJson.equals("{}")) {
                Notification.show("Koszyki - w budowie. Masz koszyki z różnych restauracji.", 3000, Notification.Position.TOP_CENTER);
            } else {
                Notification.show("Brak aktywnych koszyków", 2000, Notification.Position.TOP_CENTER);
            }
        });
    }
    
    private void handleLogout() {
        getElement().executeJs(
            "localStorage.removeItem('eatgo-token'); " +
            "localStorage.removeItem('eatgo-userId'); " +
            "localStorage.removeItem('eatgo-role');"
        );
        
        Notification.show("Wylogowano pomyślnie", 2000, Notification.Position.TOP_CENTER);
        
        // Odśwież stronę, aby zaktualizować header
        getUI().ifPresent(ui -> ui.getPage().reload());
    }
    
    private void initializeTheme() {
        try {
            getElement().executeJs(
                "const theme = localStorage.getItem('eatgo-theme') || 'light'; " +
                "document.documentElement.setAttribute('data-theme', theme); " +
                "return theme;"
            ).then(String.class, theme -> {
                if (theme != null) {
                    isDarkMode = "dark".equals(theme);
                    updateThemeToggleIcon();
                } else {
                    isDarkMode = false;
                    updateThemeToggleIcon();
                }
            });
        } catch (Exception e) {
            isDarkMode = false;
            updateThemeToggleIcon();
        }
    }
    
    private void toggleTheme() {
        isDarkMode = !isDarkMode;
        String theme = isDarkMode ? "dark" : "light";
        
        getElement().executeJs(
            "document.documentElement.setAttribute('data-theme', $0); " +
            "localStorage.setItem('eatgo-theme', $0);",
            theme
        );
        
        updateThemeToggleIcon();
        getElement().getStyle().set("transition", "background-color 0.3s ease, color 0.3s ease");
    }
    
    private void updateThemeToggleIcon() {
        if (isDarkMode) {
            themeToggle.setIcon(VaadinIcon.SUN_O.create());
            themeToggle.setAriaLabel("Przełącz na tryb jasny");
        } else {
            themeToggle.setIcon(VaadinIcon.MOON_O.create());
            themeToggle.setAriaLabel("Przełącz na tryb ciemny");
        }
    }
}

