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
import pollub.eatgo.service.TokenValidationService;

public class HeaderComponent extends Div {
    
    private Button themeToggle;
    private boolean isDarkMode = false;
    private final AuthenticationService authService;
    private final TokenValidationService tokenValidationService;
    private HorizontalLayout headerActions;
    private Div userMenuContainer;
    private Div loginButtonsContainer;
    
    public HeaderComponent(AuthenticationService authService, TokenValidationService tokenValidationService) {
        this.authService = authService;
        this.tokenValidationService = tokenValidationService;
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
        
        // Załaduj API interceptor (automatyczne dodawanie tokena do requestów)
        loadApiInterceptor();
        
        // Sprawdź ważność tokena i wyczyść jeśli nieprawidłowy
        validateAndCleanToken();
        
        // Sprawdź stan logowania i zaktualizuj UI
        checkLoginStatus();
        
        // Nasłuchuj zmian w localStorage (dla aktualizacji po zalogowaniu)
        setupStorageListener();
    }
    
    /**
     * Ładuje skrypt API interceptor, który automatycznie dodaje token do requestów API.
     */
    private void loadApiInterceptor() {
        getUI().ifPresent(ui -> {
            ui.getPage().executeJs(
                "if (!window.eatgoApiInterceptorLoaded) { " +
                "  const script = document.createElement('script'); " +
                "  script.src = '/themes/my-theme/api-interceptor.js'; " +
                "  script.onload = function() { " +
                "    console.log('[HeaderComponent] API Interceptor loaded'); " +
                "    window.eatgoApiInterceptorLoaded = true; " +
                "  }; " +
                "  script.onerror = function() { " +
                "    console.error('[HeaderComponent] Failed to load API Interceptor'); " +
                "  }; " +
                "  document.head.appendChild(script); " +
                "}"
            );
        });
    }
    
    /**
     * Sprawdza ważność tokena w localStorage i usuwa go jeśli jest nieprawidłowy.
     */
    private void validateAndCleanToken() {
        getElement().executeJs(
            "const token = localStorage.getItem('eatgo-token'); " +
            "if (token && token !== 'null' && token !== '') { " +
            "  // Sprawdź czy token wygląda na ważny (podstawowa walidacja) " +
            "  try { " +
            "    const parts = token.split('.'); " +
            "    if (parts.length !== 3) { " +
            "      console.warn('[TokenValidation] Invalid token format, clearing...'); " +
            "      localStorage.removeItem('eatgo-token'); " +
            "      localStorage.removeItem('eatgo-userId'); " +
            "      localStorage.removeItem('eatgo-role'); " +
            "      return; " +
            "    } " +
            "    // Dekoduj payload (base64) " +
            "    const payload = JSON.parse(atob(parts[1])); " +
            "    const exp = payload.exp * 1000; // Konwersja na milisekundy " +
            "    const now = Date.now(); " +
            "    if (exp < now) { " +
            "      console.warn('[TokenValidation] Token expired, clearing...'); " +
            "      localStorage.removeItem('eatgo-token'); " +
            "      localStorage.removeItem('eatgo-userId'); " +
            "      localStorage.removeItem('eatgo-role'); " +
            "      $0.$server.onTokenExpired(); " +
            "    } else { " +
            "      console.log('[TokenValidation] Token is valid'); " +
            "    } " +
            "  } catch (e) { " +
            "    console.error('[TokenValidation] Error validating token:', e); " +
            "    localStorage.removeItem('eatgo-token'); " +
            "    localStorage.removeItem('eatgo-userId'); " +
            "    localStorage.removeItem('eatgo-role'); " +
            "  } " +
            "}",
            getElement()
        );
    }
    
    /**
     * Wywoływane gdy token wygasł - aktualizuje UI.
     */
    @com.vaadin.flow.component.ClientCallable
    public void onTokenExpired() {
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                System.out.println("[TokenValidation] Token expired, updating UI");
                userMenuContainer.setVisible(false);
                loginButtonsContainer.setVisible(true);
                Notification.show("Sesja wygasła. Zaloguj się ponownie.", 3000, Notification.Position.TOP_CENTER);
            });
        });
    }
    
    private void setupStorageListener() {
        getElement().executeJs(
            "(function(element) { " +
            "  window.addEventListener('storage', function(e) { " +
            "    if (e.key === 'eatgo-token') { " +
            "      console.log('Storage event detected for eatgo-token'); " +
            "      element.$server.onLoginStatusChanged(); " +
            "    } " +
            "  }); " +
            "  // Nasłuchuj również custom event dla zmian w localStorage w tej samej karcie " +
            "  window.addEventListener('eatgo-login-changed', function() { " +
            "    console.log('eatgo-login-changed event received'); " +
            "    element.$server.onLoginStatusChanged(); " +
            "  }); " +
            "  console.log('Storage listener setup complete'); " +
            "})($0);",
            getElement()
        );
    }
    
    @com.vaadin.flow.component.ClientCallable
    public void onLoginStatusChanged() {
        // Użyj UI.access, aby upewnić się, że aktualizacja jest w odpowiednim wątku
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                System.out.println("onLoginStatusChanged called - checking login status");
                // Wywołaj checkLoginStatus w odpowiednim wątku UI
                checkLoginStatus();
            });
        });
    }
    
    // Publiczna metoda do wywołania bezpośrednio z Java (nie przez JavaScript)
    public void refreshLoginStatus() {
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                System.out.println("refreshLoginStatus called - checking login status");
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
            LoginDialog loginDialog = new LoginDialog(authService, this);
            loginDialog.open();
        });
        
        Button signUpBtn = new Button("Zarejestruj się");
        signUpBtn.addClassName("signup-btn");
        signUpBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        signUpBtn.addClickListener(e -> {
            RegisterDialog registerDialog = new RegisterDialog(authService, this);
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
                System.out.println("onLoginStatusReceived called - isLoggedIn: " + isLoggedIn + ", userId: " + userId + ", role: " + role);
                if (isLoggedIn) {
                    createUserMenu(userId, role);
                    userMenuContainer.setVisible(true);
                    loginButtonsContainer.setVisible(false);
                    System.out.println("User menu created and shown");
                } else {
                    userMenuContainer.setVisible(false);
                    loginButtonsContainer.setVisible(true);
                    System.out.println("Login buttons shown");
                }
            });
        });
    }
    
    // Publiczna metoda do bezpośredniej aktualizacji po zalogowaniu (bez odczytywania z localStorage)
    public void updateAfterLogin(String userId, String role) {
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                System.out.println("updateAfterLogin called - userId: " + userId + ", role: " + role);
                createUserMenu(userId, role);
                userMenuContainer.setVisible(true);
                loginButtonsContainer.setVisible(false);
            });
        });
    }
    
    // Metoda wywoływana przez JavaScript po zapisaniu tokena
    @com.vaadin.flow.component.ClientCallable
    public void onTokenSaved(String userId, String role) {
        System.out.println("onTokenSaved called - userId: '" + userId + "', role: '" + role + "'");
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                System.out.println("onTokenSaved - inside UI.access - userId: '" + userId + "', role: '" + role + "'");
                System.out.println("Before update - userMenuContainer visible: " + userMenuContainer.isVisible() + 
                                 ", loginButtonsContainer visible: " + loginButtonsContainer.isVisible());
                
                if (userId != null && !userId.isEmpty() && role != null && !role.isEmpty()) {
                    System.out.println("Creating user menu...");
                    createUserMenu(userId, role);
                    System.out.println("User menu created, setting visibility...");
                    
                    // Ustaw widoczność
                    userMenuContainer.setVisible(true);
                    loginButtonsContainer.setVisible(false);
                    
                    System.out.println("After update - userMenuContainer visible: " + userMenuContainer.isVisible() + 
                                     ", loginButtonsContainer visible: " + loginButtonsContainer.isVisible());
                    System.out.println("userMenuContainer children: " + userMenuContainer.getChildren().count());
                } else {
                    System.out.println("ERROR: Invalid userId or role - userId: '" + userId + "', role: '" + role + "'");
                }
            });
        });
    }
    
    private void createUserMenu(String userId, String role) {
        System.out.println("createUserMenu called - userId: " + userId + ", role: " + role);
        
        // Usuń wszystkie istniejące elementy
        userMenuContainer.removeAll();
        System.out.println("userMenuContainer cleared");
        
        // Dodaj przycisk powiadomień (tylko ikona, bez tekstu)
        Button notificationsButton = new Button(VaadinIcon.BELL.create());
        notificationsButton.addClassName("notifications-button");
        notificationsButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        notificationsButton.setTooltipText("Powiadomienia");
        notificationsButton.addClickListener(e -> {
            Notification.show("Powiadomienia - w budowie", 2000, Notification.Position.TOP_CENTER);
        });
        
        // Dodaj przycisk koszyków (wiele koszyków - jeden per restauracja)
        Button cartsButton = new Button(VaadinIcon.CART.create());
        cartsButton.addClassName("cart-button");
        cartsButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        cartsButton.setTooltipText("Koszyki");
        cartsButton.addClickListener(e -> {
            getUI().ifPresent(ui -> ui.navigate("cart"));
        });
        
        // Dodaj badge z liczbą aktywnych koszyków
        Span cartsBadge = new Span("0");
        cartsBadge.addClassName("cart-badge");
        updateCartsBadge(cartsBadge);
        
        Div cartContainer = new Div();
        cartContainer.addClassName("cart-container");
        cartContainer.add(cartsButton, cartsBadge);
        
        // MenuBar z rozwijanym menu "Profil"
        MenuBar userMenu = new MenuBar();
        userMenu.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);
        userMenu.addClassName("user-menu");
        
        // Włącz otwieranie menu po najechaniu (hover)
        userMenu.getElement().setProperty("openOnHover", true);
        
        // Główny item "Profil" z ikoną i tekstem
        var profileItem = userMenu.addItem("Profil", e -> {
            // Kliknięcie na główny item - nie robi nic, tylko rozwija menu
        });
        profileItem.addComponentAsFirst(VaadinIcon.USER.create());
        
        // Submenu dla "Profil" - wyświetla się po najechaniu
        var profileSubMenu = profileItem.getSubMenu();
        
        var settingsSubItem = profileSubMenu.addItem("Ustawienia", e -> {
            getUI().ifPresent(ui -> ui.navigate("settings"));
        });
        settingsSubItem.addComponentAsFirst(VaadinIcon.COG.create());
        
        var addressesSubItem = profileSubMenu.addItem("Adresy", e -> {
            getUI().ifPresent(ui -> ui.navigate("addresses"));
        });
        addressesSubItem.addComponentAsFirst(VaadinIcon.MAP_MARKER.create());
        
        var ordersSubItem = profileSubMenu.addItem("Zamówienia", e -> {
            getUI().ifPresent(ui -> ui.navigate("orders"));
        });
        ordersSubItem.addComponentAsFirst(VaadinIcon.LIST.create());
        
        var logoutSubItem = profileSubMenu.addItem("Wyloguj się", e -> handleLogout());
        logoutSubItem.addComponentAsFirst(VaadinIcon.SIGN_OUT.create());
        
        // Dodaj elementy do kontenera w odpowiedniej kolejności
        userMenuContainer.add(notificationsButton, cartContainer, userMenu);
        System.out.println("Elements added to userMenuContainer - children count: " + userMenuContainer.getChildren().count());
        
        // Wymuś odświeżenie UI
        System.out.println("createUserMenu completed");
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

