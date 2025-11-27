package pollub.eatgo.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import pollub.eatgo.dto.restaurant.RestaurantSummaryDto;
import pollub.eatgo.service.AuthenticationService;
import pollub.eatgo.service.OrderNotificationService;
import pollub.eatgo.service.RestaurantService;
import pollub.eatgo.service.TokenValidationService;
import pollub.eatgo.views.components.HeaderComponent;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Route("cart")
@PageTitle("EatGo - Koszyk")
public class CartView extends VerticalLayout {
    
    private final RestaurantService restaurantService;
    private final AuthenticationService authService;
    private final TokenValidationService tokenValidationService;
    private Div cartsContainer;
    
    public CartView(RestaurantService restaurantService,
                    AuthenticationService authService,
                    TokenValidationService tokenValidationService,
                    OrderNotificationService orderNotificationService) {
        this.restaurantService = restaurantService;
        this.authService = authService;
        this.tokenValidationService = tokenValidationService;
        
        setSizeFull();
        setSpacing(false);
        setPadding(false);
        addClassName("cart-view");
        
        HeaderComponent header = new HeaderComponent(authService, tokenValidationService, orderNotificationService);
        add(header);
        
        add(createContent());
        
        loadCarts();
    }
    
    private Div createContent() {
        Div content = new Div();
        content.addClassName("cart-content");
        
        H2 title = new H2("Koszyk");
        title.addClassName("cart-title");
        
        cartsContainer = new Div();
        cartsContainer.addClassName("carts-container");
        
        content.add(title, cartsContainer);
        return content;
    }
    
    private void loadCarts() {
        displayCarts();
    }
    
    private void displayCarts() {
        cartsContainer.removeAll();
        
               getElement().executeJs(
                   "const carts = JSON.parse(localStorage.getItem('eatgo-carts') || '{}'); " +
                   "const restaurantIds = Object.keys(carts).filter(id => " +
                   "  carts[id].items && carts[id].items.length > 0" +
                   "); " +
                   "console.log('CartView.displayCarts: Found ' + restaurantIds.length + ' restaurants with items'); " +
                   "if (restaurantIds.length === 0) { " +
                   "  $0.$server.showEmptyCart(); " +
                   "} else { " +
                   "  restaurantIds.forEach(restaurantId => { " +
                   "    const cart = carts[restaurantId]; " +
                   "    const subtotal = cart.items.reduce((sum, item) => sum + (item.price * item.quantity), 0); " +
                   "    console.log('CartView.displayCarts: Creating card for restaurant ' + restaurantId + ' with subtotal ' + subtotal); " +
                   "    $0.$server.createCartCard(String(restaurantId), JSON.stringify(cart.items), String(subtotal)); " +
                   "  }); " +
                   "}",
                   getElement()
               );
    }
    
    @com.vaadin.flow.component.ClientCallable
    public void createCartCard(String restaurantIdStr, String itemsJson, String subtotalStr) {
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                try {
                    Long restaurantId = Long.parseLong(restaurantIdStr);
                    double subtotal = Double.parseDouble(subtotalStr);
                    
                    RestaurantSummaryDto restaurant = restaurantService.listRestaurants().stream()
                            .filter(r -> r.id().equals(restaurantId))
                            .findFirst()
                            .orElse(null);
                    
                    if (restaurant == null) {
                        System.err.println("CartView.createCartCard: Restaurant not found for ID: " + restaurantId);
                        return;
                    }
                    
                    Div card = createRestaurantCartCard(restaurant, itemsJson, subtotal);
                    cartsContainer.add(card);
                } catch (NumberFormatException e) {
                    System.err.println("CartView.createCartCard: Error parsing restaurantId or subtotal: " + e.getMessage());
                }
            });
        });
    }
    
    private Div createRestaurantCartCard(RestaurantSummaryDto restaurant, String itemsJson, double subtotal) {
        Div card = new Div();
        card.addClassName("restaurant-cart-card");
        card.getStyle().set("cursor", "pointer");
        
        Div imageDiv = new Div();
        imageDiv.addClassName("restaurant-cart-image");
        if (restaurant.imageUrl() != null && !restaurant.imageUrl().isEmpty()) {
            Image img = new Image(restaurant.imageUrl(), restaurant.name());
            img.addClassName("restaurant-cart-img");
            imageDiv.add(img);
        }
        
        Div content = new Div();
        content.addClassName("restaurant-cart-content");
        
        H3 name = new H3(restaurant.name());
        name.addClassName("restaurant-cart-name");
        
        double deliveryPrice = restaurant.deliveryPrice() != null ?
            restaurant.deliveryPrice().doubleValue() : 0.0;
        double total = subtotal + deliveryPrice;
        
        Span subtotalSpan = new Span("Suma pośrednia: " + String.format("%.2f zł", subtotal));
        subtotalSpan.addClassName("restaurant-cart-subtotal");
        
        Span deliveryPriceSpan = new Span("Dostawa: " + String.format("%.2f zł", deliveryPrice));
        deliveryPriceSpan.addClassName("restaurant-cart-delivery-price");
        
        Span totalSpan = new Span("Suma całkowita: " + String.format("%.2f zł", total));
        totalSpan.addClassName("restaurant-cart-total");
        totalSpan.getStyle().set("font-weight", "bold");
        
        Span deliveryInfo = new Span("Dostawa na adres: " + restaurant.address());
        deliveryInfo.addClassName("restaurant-cart-delivery");
        
        Span status = new Span("Zamknięte • Lokal dostępny: Czwartek od 8:00AM");
        status.addClassName("restaurant-cart-status");
        
        HorizontalLayout bottom = new HorizontalLayout();
        bottom.addClassName("restaurant-cart-bottom");
        bottom.setWidthFull();
        bottom.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);
        bottom.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        
        Span badge = new Span("0");
        badge.addClassName("restaurant-cart-badge");
        
        getElement().executeJs(
            "const items = JSON.parse($0); " +
            "const itemCount = items.reduce((sum, item) => sum + item.quantity, 0); " +
            "return itemCount;",
            itemsJson
        ).then(Integer.class, count -> {
            if (count != null) {
                badge.setText(String.valueOf(count));
            }
        });
        
        Button checkoutBtn = new Button("Zamów", VaadinIcon.ARROW_RIGHT.create());
        checkoutBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        checkoutBtn.addClickListener(e -> {
            getUI().ifPresent(ui -> ui.navigate("checkout/" + restaurant.id()));
        });
        
        bottom.add(badge, checkoutBtn);
        
        content.add(name, subtotalSpan, deliveryPriceSpan, totalSpan, deliveryInfo, status, bottom);
        card.add(imageDiv, content);
        
        card.addClickListener(e -> {
            if (!e.getSource().equals(checkoutBtn)) {
                getUI().ifPresent(ui -> ui.navigate("checkout/" + restaurant.id()));
            }
        });
        
        return card;
    }
    
    @com.vaadin.flow.component.ClientCallable
    public void setCartBadge(Long restaurantId, int count) {
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                getElement().executeJs(
                    "const badge = document.getElementById('cart-badge-' + $0); " +
                    "if (badge) badge.textContent = $1;",
                    restaurantId, count
                );
            });
        });
    }
}

