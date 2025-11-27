package pollub.eatgo.views.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLink;
import pollub.eatgo.service.AuthenticationService;
import pollub.eatgo.service.OrderNotificationService;
import pollub.eatgo.service.TokenValidationService;

public class HeaderComponent extends Div {
    
    private Button themeToggle;
    private boolean isDarkMode = false;
    private final AuthenticationService authService;
    private final TokenValidationService tokenValidationService;
    private final OrderNotificationService orderNotificationService;
    private HorizontalLayout headerActions;
    private Div userMenuContainer;
    private Div loginButtonsContainer;
    private Span notificationsBadge;
    private Dialog notificationsDialog;
    private java.util.concurrent.ScheduledExecutorService badgeRefreshExecutor;
    
    public HeaderComponent(AuthenticationService authService,
                           TokenValidationService tokenValidationService,
                           OrderNotificationService orderNotificationService) {
        this.authService = authService;
        this.tokenValidationService = tokenValidationService;
        this.orderNotificationService = orderNotificationService;
        addClassName("home-header");
        setWidthFull();
        
        Div headerContent = new Div();
        headerContent.addClassName("header-content");
        headerContent.setWidthFull();
        
        Div logoContainer = new Div();
        logoContainer.addClassName("logo-container");
        
        Div logoLink = new Div();
        logoLink.addClassName("logo-link");
        logoLink.getStyle().set("cursor", "pointer");
        logoLink.addClickListener(e -> navigateToHomeOrDashboard());
        
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
        
        userMenuContainer = new Div();
        userMenuContainer.addClassName("user-menu-container");
        userMenuContainer.setVisible(false);
        
        loginButtonsContainer = new Div();
        loginButtonsContainer.addClassName("login-buttons-container");
        createLoginButtons();
        
        headerActions.add(themeToggle, userMenuContainer, loginButtonsContainer);
        headerContent.add(logoContainer, headerActions);
        add(headerContent);
        
        loadApiInterceptor();
        
        validateAndCleanToken();
        
        checkLoginStatus();
        
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
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                System.out.println("onLoginStatusChanged called - checking login status");
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
        
        userMenuContainer.removeAll();
        System.out.println("userMenuContainer cleared");
        
        Long parsedUserId = null;
        try {
            if (userId != null && !userId.isBlank()) {
                parsedUserId = Long.parseLong(userId);
            }
        } catch (NumberFormatException ex) {
            System.err.println("HeaderComponent: invalid userId format: " + userId);
        }

        Button notificationsButton = new Button(VaadinIcon.BELL.create());
        notificationsButton.addClassName("notifications-button");
        notificationsButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        notificationsButton.setTooltipText("Powiadomienia");

        notificationsBadge = new Span();
        notificationsBadge.addClassName("notifications-badge");
        notificationsBadge.setVisible(false);

        if (parsedUserId != null) {
            getUI().ifPresent(ui -> {
                ui.access(() -> {
                    ui.getPage().executeJs("setTimeout(function() { $0.$server.refreshNotificationsBadge(); }, 200);", getElement());
                });
            });
            startBadgeRefreshTimer(parsedUserId);
        }

        Div notificationsContainer = new Div();
        notificationsContainer.addClassName("notifications-container");
        notificationsContainer.add(notificationsButton, notificationsBadge);

        Long finalParsedUserId = parsedUserId;
        notificationsButton.addClickListener(e -> {
            if (finalParsedUserId != null) {
                openNotificationsDialog(finalParsedUserId);
            } else {
                Notification.show("Brak powiązanego użytkownika dla powiadomień", 3000, Notification.Position.TOP_CENTER);
            }
        });
        
        MenuBar userMenu = new MenuBar();
        userMenu.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);
        userMenu.addClassName("user-menu");
        
        userMenu.getElement().setProperty("openOnHover", true);
        
        String menuLabel = "CLIENT".equalsIgnoreCase(role) ? "Profil" :
                          "RESTAURANT_ADMIN".equalsIgnoreCase(role) ? "Restauracja" : "Profil";
        
        var profileItem = userMenu.addItem(menuLabel, e -> {
        });
        profileItem.addComponentAsFirst(VaadinIcon.USER.create());
        
        var profileSubMenu = profileItem.getSubMenu();
        
        if ("CLIENT".equalsIgnoreCase(role)) {
            Button cartsButton = new Button(VaadinIcon.CART.create());
            cartsButton.addClassName("cart-button");
            cartsButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
            cartsButton.setTooltipText("Koszyki");
            cartsButton.addClickListener(e -> {
                getUI().ifPresent(ui -> ui.navigate("cart"));
            });
            
            Span cartsBadge = new Span("0");
            cartsBadge.addClassName("cart-badge");
            updateCartsBadge(cartsBadge);
            
            Div cartContainer = new Div();
            cartContainer.addClassName("cart-container");
            cartContainer.add(cartsButton, cartsBadge);
            
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
            
            userMenuContainer.add(notificationsContainer, cartContainer, userMenu);
        } else if ("RESTAURANT_ADMIN".equalsIgnoreCase(role)) {
            var dashboardSubItem = profileSubMenu.addItem("Panel restauracji", e -> {
                getUI().ifPresent(ui -> ui.navigate("restaurant"));
            });
            dashboardSubItem.addComponentAsFirst(VaadinIcon.HOME.create());
            
            var settingsSubItem = profileSubMenu.addItem("Ustawienia", e -> {
                getUI().ifPresent(ui -> ui.navigate("restaurant/settings"));
            });
            settingsSubItem.addComponentAsFirst(VaadinIcon.COG.create());
            
            var logoutSubItem = profileSubMenu.addItem("Wyloguj się", e -> handleLogout());
            logoutSubItem.addComponentAsFirst(VaadinIcon.SIGN_OUT.create());
            
            userMenuContainer.add(notificationsContainer, userMenu);
        } else if ("COURIER".equalsIgnoreCase(role)) {
            var dashboardSubItem = profileSubMenu.addItem("Panel kuriera", e -> {
                getUI().ifPresent(ui -> ui.navigate("courier"));
            });
            dashboardSubItem.addComponentAsFirst(VaadinIcon.TRUCK.create());
            
            var logoutSubItem = profileSubMenu.addItem("Wyloguj się", e -> handleLogout());
            logoutSubItem.addComponentAsFirst(VaadinIcon.SIGN_OUT.create());
            
            userMenuContainer.add(notificationsContainer, userMenu);
        } else {
            var logoutSubItem = profileSubMenu.addItem("Wyloguj się", e -> handleLogout());
            logoutSubItem.addComponentAsFirst(VaadinIcon.SIGN_OUT.create());
            
            userMenuContainer.add(notificationsContainer, userMenu);
        }
        
        System.out.println("Elements added to userMenuContainer - children count: " + userMenuContainer.getChildren().count());
        
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

    private void updateNotificationsBadge(Long userId) {
        if (notificationsBadge == null || userId == null) {
            return;
        }
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                try {
                    long unread = orderNotificationService.countUnread(userId);
                    System.out.println("HeaderComponent: Unread notifications count for user " + userId + ": " + unread);
                    if (unread > 0) {
                        notificationsBadge.setText(String.valueOf(unread));
                        notificationsBadge.setVisible(true);
                        System.out.println("HeaderComponent: Badge set to visible with count: " + unread);
                    } else {
                        notificationsBadge.setVisible(false);
                        System.out.println("HeaderComponent: Badge hidden (no unread notifications)");
                    }
                } catch (Exception e) {
                    System.err.println("HeaderComponent: Error updating notifications badge: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        });
    }
    
    @com.vaadin.flow.component.ClientCallable
    public void refreshNotificationsBadge() {
        getElement().executeJs(
            "const userId = localStorage.getItem('eatgo-userId'); " +
            "if (userId && userId !== 'null' && userId !== '') { " +
            "  $0.$server.updateNotificationsBadgeFromClient(userId); " +
            "}"
        );
    }
    
    @com.vaadin.flow.component.ClientCallable
    public void updateNotificationsBadgeFromClient(String userIdStr) {
        try {
            if (userIdStr != null && !userIdStr.isEmpty()) {
                Long userId = Long.parseLong(userIdStr);
                updateNotificationsBadge(userId);
            }
        } catch (NumberFormatException e) {
            System.err.println("HeaderComponent: Invalid userId format: " + userIdStr);
        }
    }
    
    private void startBadgeRefreshTimer(Long userId) {
        if (badgeRefreshExecutor != null) {
            badgeRefreshExecutor.shutdown();
        }
        
        badgeRefreshExecutor = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
        badgeRefreshExecutor.scheduleAtFixedRate(
            () -> {
                getUI().ifPresent(ui -> {
                    ui.access(() -> updateNotificationsBadge(userId));
                });
            },
            3,
            3,
            java.util.concurrent.TimeUnit.SECONDS
        );
    }

    private void openNotificationsDialog(Long userId) {
        var notifications = orderNotificationService.getNotificationsForUser(userId);

        if (notificationsDialog == null) {
            notificationsDialog = new Dialog();
            notificationsDialog.setHeaderTitle("Powiadomienia");
            notificationsDialog.setModal(false);
            notificationsDialog.setDraggable(true);
            notificationsDialog.setResizable(false);
            
            Button closeButton = new Button(VaadinIcon.CLOSE_SMALL.create(), event -> {
                notificationsDialog.close();
                if (userId != null) {
                    updateNotificationsBadge(userId);
                }
            });
            closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
            closeButton.getElement().getStyle().set("margin-left", "auto");
            notificationsDialog.getHeader().add(closeButton);
        } else {
            notificationsDialog.removeAll();
        }

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setWidthFull();

        if (notifications.isEmpty()) {
            Paragraph empty = new Paragraph("Brak powiadomień o zamówieniach.");
            empty.getStyle().set("margin", "0.5rem 0");
            layout.add(empty);
        } else {
            for (var n : notifications) {
                Paragraph p = new Paragraph();
                String text = String.format(
                        "[%s] %s",
                        n.createdAt() != null
                                ? n.createdAt().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                                : "",
                        n.message()
                );
                p.setText(text);
                p.getStyle().set("margin", "0.25rem 0");
                layout.add(p);
            }
        }

        notificationsDialog.add(layout);
        
        notificationsDialog.addOpenedChangeListener(e -> {
            if (e.isOpened() && userId != null) {
                orderNotificationService.markAllAsRead(userId);
                getUI().ifPresent(ui -> {
                ui.access(() -> {
                    ui.getPage().executeJs("setTimeout(function() { $0.$server.updateNotificationsBadgeFromClient('" + userId + "'); }, 100);", getElement());
                });
                });
            }
        });
        
        notificationsDialog.open();
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
    
    private void navigateToHomeOrDashboard() {
        getElement().executeJs(
            "const role = localStorage.getItem('eatgo-role'); " +
            "if (role === 'RESTAURANT_ADMIN') { " +
            "  window.location.href = '/restaurant'; " +
            "} else if (role === 'COURIER') { " +
            "  window.location.href = '/courier'; " +
            "} else { " +
            "  window.location.href = '/'; " +
            "}"
        );
    }
}

