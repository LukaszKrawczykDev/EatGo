package pollub.eatgo.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import pollub.eatgo.dto.restaurant.RestaurantDto;
import pollub.eatgo.dto.restaurant.RestaurantUpdateDto;
import pollub.eatgo.service.AuthenticationService;
import pollub.eatgo.service.OrderNotificationService;
import pollub.eatgo.service.RestaurantService;
import pollub.eatgo.service.TokenValidationService;
import pollub.eatgo.views.components.HeaderComponent;

@Route("restaurant/settings")
@PageTitle("Ustawienia Restauracji - EatGo")
public class RestaurantSettingsView extends VerticalLayout implements BeforeEnterObserver {

    private final AuthenticationService authService;
    private final RestaurantService restaurantService;
    private final TokenValidationService tokenValidationService;

    private String adminEmail;
    private RestaurantDto restaurant;

    private TextField nameField;
    private TextField addressField;
    private NumberField deliveryPriceField;
    private TextField imageUrlField;

    public RestaurantSettingsView(AuthenticationService authService,
                                  RestaurantService restaurantService,
                                  TokenValidationService tokenValidationService,
                                  OrderNotificationService orderNotificationService) {
        this.authService = authService;
        this.restaurantService = restaurantService;
        this.tokenValidationService = tokenValidationService;

        setSizeFull();
        setSpacing(false);
        setPadding(false);
        addClassName("restaurant-settings-view");

        HeaderComponent header = new HeaderComponent(authService, tokenValidationService, orderNotificationService);
        add(header);

        Div content = new Div();
        content.addClassName("settings-content");
        content.setWidthFull();
        content.setMaxWidth("800px");
        content.getStyle().set("margin", "0 auto");
        content.getStyle().set("padding", "20px");

        VerticalLayout formLayout = new VerticalLayout();
        formLayout.setSpacing(true);
        formLayout.setPadding(true);

        H2 title = new H2("Ustawienia restauracji");

        nameField = new TextField("Nazwa restauracji");
        nameField.setWidthFull();

        addressField = new TextField("Adres");
        addressField.setWidthFull();

        deliveryPriceField = new NumberField("Cena dostawy (zł)");
        deliveryPriceField.setWidthFull();
        
        imageUrlField = new TextField("Link do zdjęcia restauracji (URL)");
        imageUrlField.setWidthFull();
        imageUrlField.setPlaceholder("https://example.com/restaurant-image.jpg");

        Button saveBtn = new Button("Zapisz", e -> saveSettings());
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        formLayout.add(title, nameField, addressField, deliveryPriceField, imageUrlField, saveBtn);
        content.add(formLayout);
        add(content);

        // Load email from token
        loadAdminEmail();
    }

    private void loadAdminEmail() {
        getElement().executeJs(
            "const token = localStorage.getItem('eatgo-token'); " +
            "if (token && token !== 'null') { " +
            "  try { " +
            "    const parts = token.split('.'); " +
            "    if (parts.length === 3) { " +
            "      const payload = JSON.parse(atob(parts[1])); " +
            "      const email = payload.email; " +
            "      $0.$server.setAdminEmail(email); " +
            "    } " +
            "  } catch (e) { " +
            "    console.error('Error extracting email from token:', e); " +
            "  } " +
            "}"
        );
    }

    @com.vaadin.flow.component.ClientCallable
    public void setAdminEmail(String email) {
        this.adminEmail = email;
        if (email != null) {
            loadRestaurantData();
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        getElement().executeJs(
            "const token = localStorage.getItem('eatgo-token'); " +
            "const role = localStorage.getItem('eatgo-role'); " +
            "if (!token || token === 'null' || !role || role !== 'RESTAURANT_ADMIN') { " +
            "  window.location.href = '/'; " +
            "}"
        );
    }

    private String getAdminEmail() {
        if (adminEmail != null) {
            return adminEmail;
        }
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null) {
                return auth.getName();
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    private void loadRestaurantData() {
        try {
            String email = getAdminEmail();
            if (email == null) {
                Notification.show("Błąd autoryzacji", 5000, Notification.Position.TOP_CENTER);
                return;
            }
            restaurant = restaurantService.getRestaurantForAdmin(email);
            if (restaurant != null) {
                nameField.setValue(restaurant.getName());
                addressField.setValue(restaurant.getAddress());
                deliveryPriceField.setValue(restaurant.getDeliveryPrice());
                if (restaurant.getImageUrl() != null) {
                    imageUrlField.setValue(restaurant.getImageUrl());
                }
            }
        } catch (Exception e) {
            Notification.show("Błąd podczas ładowania danych: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER);
        }
    }

    private void saveSettings() {
        try {
            String email = getAdminEmail();
            if (email == null) {
                Notification.show("Błąd autoryzacji", 3000, Notification.Position.TOP_CENTER);
                return;
            }
            
            RestaurantUpdateDto dto = new RestaurantUpdateDto(
                nameField.getValue(),
                addressField.getValue(),
                deliveryPriceField.getValue(),
                imageUrlField.getValue()
            );
            
            restaurant = restaurantService.updateRestaurant(email, dto);
            Notification.show("Ustawienia zaktualizowane", 3000, Notification.Position.TOP_CENTER);
        } catch (Exception e) {
            Notification.show("Błąd: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER);
        }
    }
}

