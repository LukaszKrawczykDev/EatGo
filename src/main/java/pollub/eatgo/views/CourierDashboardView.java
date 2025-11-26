package pollub.eatgo.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.fasterxml.jackson.databind.ObjectMapper;
import pollub.eatgo.dto.order.OrderDetailsDto;
import pollub.eatgo.dto.address.AddressDto;
import pollub.eatgo.dto.restaurant.RestaurantSummaryDto;
import pollub.eatgo.service.AuthenticationService;
import pollub.eatgo.service.TokenValidationService;
import pollub.eatgo.views.components.HeaderComponent;

import java.util.ArrayList;
import java.util.List;

@Route("courier")
@PageTitle("EatGo - Panel Kuriera")
public class CourierDashboardView extends VerticalLayout implements BeforeEnterObserver {
    
    private final AuthenticationService authService;
    private final TokenValidationService tokenValidationService;
    private final ObjectMapper objectMapper;
    
    private Div ordersContent;
    private HorizontalLayout activeOrdersContainer;
    private HorizontalLayout deliveredOrdersContainer;
    private Button refreshButton;
    
    public CourierDashboardView(AuthenticationService authService, TokenValidationService tokenValidationService) {
        this.authService = authService;
        this.tokenValidationService = tokenValidationService;
        
        // Inicjalizacja ObjectMapper z obsługą dat Java 8
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        this.objectMapper.disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        
        setSizeFull();
        setSpacing(false);
        setPadding(false);
        addClassName("courier-dashboard-view");
        getStyle().set("background-color", "var(--bg-secondary)");
        
        HeaderComponent headerComponent = new HeaderComponent(authService, tokenValidationService);
        add(headerComponent);
        
        Div content = new Div();
        content.addClassName("courier-dashboard-content");
        content.getStyle().set("max-width", "1200px");
        content.getStyle().set("margin", "0 auto");
        content.getStyle().set("padding", "2rem");
        content.getStyle().set("background-color", "var(--bg-secondary)");
        content.getStyle().set("min-height", "calc(100vh - 80px)");
        content.getStyle().set("position", "relative");
        
        // Przycisk odświeżania w prawym górnym rogu
        refreshButton = new Button("Odśwież", VaadinIcon.REFRESH.create());
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.getStyle().set("position", "absolute");
        refreshButton.getStyle().set("top", "2rem");
        refreshButton.getStyle().set("right", "2rem");
        refreshButton.addClickListener(e -> {
            loadOrders();
        });
        content.add(refreshButton);
        
        // Orders content
        ordersContent = new Div();
        ordersContent.addClassName("courier-orders-content");
        ordersContent.setWidthFull();
        
        Div activeSection = new Div();
        activeSection.addClassName("courier-section");
        activeSection.setWidthFull();
        
        H3 activeTitle = new H3("Aktywne dostawy");
        activeTitle.addClassName("courier-section-title");
        
        // Poziomy kontener z przewijaniem dla aktywnych zamówień
        Div activeOrdersScrollContainer = new Div();
        activeOrdersScrollContainer.addClassName("orders-scroll-container");
        activeOrdersScrollContainer.setWidthFull();
        
        activeOrdersContainer = new HorizontalLayout();
        activeOrdersContainer.addClassName("orders-horizontal-container");
        activeOrdersContainer.setSpacing(true);
        activeOrdersContainer.setPadding(false);
        activeOrdersContainer.setWidth(null);
        activeOrdersContainer.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.STRETCH);
        
        activeOrdersScrollContainer.add(activeOrdersContainer);
        activeSection.add(activeTitle, activeOrdersScrollContainer);
        
        Div deliveredSection = new Div();
        deliveredSection.addClassName("courier-section");
        deliveredSection.getStyle().set("margin-top", "3rem");
        deliveredSection.setWidthFull();
        
        H3 deliveredTitle = new H3("Zakończone dostawy");
        deliveredTitle.addClassName("courier-section-title");
        
        // Poziomy kontener z przewijaniem dla zakończonych zamówień
        Div deliveredOrdersScrollContainer = new Div();
        deliveredOrdersScrollContainer.addClassName("orders-scroll-container");
        deliveredOrdersScrollContainer.setWidthFull();
        
        deliveredOrdersContainer = new HorizontalLayout();
        deliveredOrdersContainer.addClassName("orders-horizontal-container");
        deliveredOrdersContainer.setSpacing(true);
        deliveredOrdersContainer.setPadding(false);
        deliveredOrdersContainer.setWidth(null);
        deliveredOrdersContainer.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.STRETCH);
        
        deliveredOrdersScrollContainer.add(deliveredOrdersContainer);
        deliveredSection.add(deliveredTitle, deliveredOrdersScrollContainer);
        
        ordersContent.add(activeSection, deliveredSection);
        
        content.add(ordersContent);
        add(content);
    }
    
    @Override
    protected void onAttach(com.vaadin.flow.component.AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        
        // Sprawdź autoryzację po stronie klienta
        getElement().executeJs(
            "const token = localStorage.getItem('eatgo-token'); " +
            "const role = localStorage.getItem('eatgo-role'); " +
            "if (!token || token === 'null' || !role || role !== 'COURIER') { " +
            "  window.location.href = '/'; " +
            "}"
        );
        
        // Załaduj zamówienia po pełnym załadowaniu komponentu
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                ui.getPage().executeJs("setTimeout(function() { $0.$server.loadOrdersDelayed(); }, 200);", getElement());
            });
        });
    }
    
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Tylko sprawdź autoryzację, nie ładuj tutaj
    }
    
    @com.vaadin.flow.component.ClientCallable
    public void loadOrdersDelayed() {
        loadOrders();
    }
    
    private void loadOrders() {
        try {
            getElement().executeJs(
                "const token = localStorage.getItem('eatgo-token'); " +
                "if (!token) { " +
                "  $0.$server.onError('Musisz być zalogowany, aby zobaczyć zamówienia'); " +
                "  return; " +
                "} " +
                "fetch('/api/courier/orders', { " +
                "  headers: { 'Authorization': 'Bearer ' + token } " +
                "}) " +
                ".then(r => { " +
                "  if (!r.ok) { " +
                "    return r.text().then(text => { " +
                "      throw new Error('HTTP ' + r.status + ': ' + (text || r.statusText)); " +
                "    }); " +
                "  } " +
                "  return r.json(); " +
                "}) " +
                ".then(orders => { " +
                "  console.log('CourierDashboard: Loaded ' + orders.length + ' orders'); " +
                "  $0.$server.displayOrders(JSON.stringify(orders)); " +
                "}) " +
                ".catch(e => { " +
                "  console.error('Error loading orders:', e); " +
                "  $0.$server.onError('Błąd podczas ładowania zamówień: ' + e.message); " +
                "});",
                getElement()
            );
        } catch (Exception e) {
            System.err.println("CourierDashboard: Error in loadOrders: " + e.getMessage());
            e.printStackTrace();
            getUI().ifPresent(ui -> {
                ui.access(() -> {
                    Notification.show("Błąd podczas ładowania zamówień: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER);
                });
            });
        }
    }
    
    @com.vaadin.flow.component.ClientCallable
    public void displayOrders(String ordersJson) {
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                try {
                    if (ordersJson == null || ordersJson.trim().isEmpty() || ordersJson.equals("null")) {
                        showEmptyOrdersState();
                        return;
                    }
                    
                    if (!ordersJson.trim().startsWith("[")) {
                        Notification.show("Błąd: Nieprawidłowy format danych", 3000, Notification.Position.TOP_CENTER);
                        showEmptyOrdersState();
                        return;
                    }
                    
                    List<OrderDetailsDto> orders = objectMapper.readValue(ordersJson, 
                        objectMapper.getTypeFactory().constructCollectionType(List.class, OrderDetailsDto.class));
                    
                    activeOrdersContainer.removeAll();
                    deliveredOrdersContainer.removeAll();
                    
                    List<OrderDetailsDto> activeOrders = new ArrayList<>();
                    List<OrderDetailsDto> deliveredOrders = new ArrayList<>();
                    
                    for (OrderDetailsDto order : orders) {
                        if (isDelivered(order.status())) {
                            deliveredOrders.add(order);
                        } else {
                            activeOrders.add(order);
                        }
                    }
                    
                    if (activeOrders.isEmpty()) {
                        Div emptyMsg = new Div();
                        emptyMsg.setText("Brak aktywnych dostaw");
                        emptyMsg.addClassName("empty-state-message");
                        emptyMsg.getStyle().set("width", "100%");
                        activeOrdersContainer.removeAll();
                        activeOrdersContainer.add(emptyMsg);
                    } else {
                        activeOrdersContainer.removeAll();
                        for (OrderDetailsDto order : activeOrders) {
                            activeOrdersContainer.add(createOrderCard(order, true));
                        }
                    }
                    
                    if (deliveredOrders.isEmpty()) {
                        Div emptyMsg = new Div();
                        emptyMsg.setText("Brak zakończonych dostaw");
                        emptyMsg.addClassName("empty-state-message");
                        emptyMsg.getStyle().set("width", "100%");
                        deliveredOrdersContainer.removeAll();
                        deliveredOrdersContainer.add(emptyMsg);
                    } else {
                        deliveredOrdersContainer.removeAll();
                        for (OrderDetailsDto order : deliveredOrders) {
                            deliveredOrdersContainer.add(createOrderCard(order, false));
                        }
                    }
                } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                    System.err.println("CourierDashboard: JSON parsing error: " + e.getMessage());
                    e.printStackTrace();
                    Notification.show("Błąd podczas parsowania danych zamówień: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER);
                    showEmptyOrdersState();
                } catch (Exception e) {
                    System.err.println("CourierDashboard: Error displaying orders: " + e.getMessage());
                    e.printStackTrace();
                    Notification.show("Błąd podczas wyświetlania zamówień: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER);
                    showEmptyOrdersState();
                }
            });
        });
    }
    
    private void showEmptyOrdersState() {
        activeOrdersContainer.removeAll();
        deliveredOrdersContainer.removeAll();
        
        Div emptyMsg = new Div();
        emptyMsg.setText("Brak zamówień");
        emptyMsg.addClassName("empty-state-message");
        activeOrdersContainer.add(emptyMsg);
    }
    
    private boolean isDelivered(String status) {
        if (status == null) return false;
        return status.equalsIgnoreCase("DELIVERED") || status.equalsIgnoreCase("CANCELLED");
    }
    
    private Div createOrderCard(OrderDetailsDto order, boolean isActive) {
        Div card = new Div();
        card.addClassName("courier-order-card");
        
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(false);
        content.setPadding(false);
        
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        
        Div orderInfo = new Div();
        Span orderNumber = new Span("#" + order.id());
        orderNumber.addClassName("order-number");
        
        Span orderDate = new Span(formatDateFromOffset(order.createdAt()));
        orderDate.addClassName("order-date");
        
        orderInfo.add(orderNumber, orderDate);
        
        Span statusBadge = new Span(getStatusLabel(order.status()));
        statusBadge.addClassName("order-status-badge");
        statusBadge.getStyle().set("padding", "0.5rem 1rem");
        statusBadge.getStyle().set("border-radius", "20px");
        statusBadge.getStyle().set("font-size", "0.875rem");
        statusBadge.getStyle().set("font-weight", "bold");
        statusBadge.getStyle().set("background", getStatusBackgroundColor(order.status()));
        statusBadge.getStyle().set("color", getStatusColor(order.status()));
        
        header.add(orderInfo, statusBadge);
        content.add(header);
        
        // ADRES DOSTAWY - NAJWAŻNIEJSZE!
        if (order.deliveryAddress() != null) {
            Div addressSection = new Div();
            addressSection.addClassName("delivery-address-section");
            addressSection.getStyle().set("background", "var(--bg-secondary)");
            addressSection.getStyle().set("padding", "1rem");
            addressSection.getStyle().set("border-radius", "8px");
            addressSection.getStyle().set("margin-top", "1rem");
            addressSection.getStyle().set("border", "2px solid var(--accent-color)");
            
            Span addressLabel = new Span("Adres dostawy:");
            addressLabel.addClassName("address-label");
            addressLabel.getStyle().set("font-weight", "bold");
            addressLabel.getStyle().set("display", "block");
            addressLabel.getStyle().set("margin-bottom", "0.5rem");
            addressLabel.getStyle().set("color", "var(--accent-color)");
            
            AddressDto addr = order.deliveryAddress();
            String addressText = addr.street() + ", " + addr.postalCode() + " " + addr.city();
            if (addr.apartmentNumber() != null && !addr.apartmentNumber().trim().isEmpty()) {
                addressText += ", lok. " + addr.apartmentNumber();
            }
            Span addressValue = new Span(addressText);
            addressValue.addClassName("address-value");
            addressValue.getStyle().set("font-size", "1.1rem");
            addressValue.getStyle().set("display", "block");
            
            addressSection.add(addressLabel, addressValue);
            content.add(addressSection);
        }
        
        // Restauracja
        if (order.restaurant() != null) {
            Div restaurantSection = new Div();
            restaurantSection.addClassName("restaurant-section");
            restaurantSection.getStyle().set("margin-top", "0.75rem");
            
            Span restaurantLabel = new Span("Restauracja: ");
            restaurantLabel.addClassName("restaurant-label");
            restaurantLabel.getStyle().set("font-weight", "bold");
            
            Span restaurantName = new Span(order.restaurant().name());
            restaurantName.addClassName("restaurant-name");
            
            restaurantSection.add(restaurantLabel, restaurantName);
            content.add(restaurantSection);
        }
        
        // Lista produktów (rozwijana)
        if (order.items() != null && !order.items().isEmpty()) {
            int totalItems = order.items().stream()
                .mapToInt(item -> item.quantity())
                .sum();
            
            Button toggleItemsButton = new Button(totalItems + " " + (totalItems == 1 ? "produkt" : "produktów") + " - Pokaż szczegóły");
            toggleItemsButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            toggleItemsButton.getStyle().set("margin-top", "0.75rem");
            toggleItemsButton.getStyle().set("width", "100%");
            toggleItemsButton.getStyle().set("justify-content", "flex-start");
            
            Div itemsContainer = new Div();
            itemsContainer.addClassName("order-items-container");
            itemsContainer.setVisible(false);
            itemsContainer.getStyle().set("margin-top", "0.5rem");
            itemsContainer.getStyle().set("padding", "0.75rem");
            itemsContainer.getStyle().set("background", "var(--bg-secondary)");
            itemsContainer.getStyle().set("border-radius", "6px");
            
            VerticalLayout itemsList = new VerticalLayout();
            itemsList.setSpacing(false);
            itemsList.setPadding(false);
            
            for (var item : order.items()) {
                Div itemRow = new Div();
                itemRow.getStyle().set("display", "flex");
                itemRow.getStyle().set("justify-content", "space-between");
                itemRow.getStyle().set("padding", "0.5rem 0");
                itemRow.getStyle().set("border-bottom", "1px solid var(--border-color)");
                
                Span itemName = new Span(item.quantity() + "x " + item.dishName());
                itemName.getStyle().set("flex-grow", "1");
                
                Span itemPrice = new Span(String.format("%.2f zł", item.priceSnapshot() * item.quantity()));
                itemPrice.getStyle().set("font-weight", "bold");
                itemPrice.getStyle().set("margin-left", "1rem");
                
                itemRow.add(itemName, itemPrice);
                itemsList.add(itemRow);
            }
            
            itemsContainer.add(itemsList);
            
            toggleItemsButton.addClickListener(e -> {
                boolean isVisible = itemsContainer.isVisible();
                itemsContainer.setVisible(!isVisible);
                toggleItemsButton.setText(isVisible ? 
                    totalItems + " " + (totalItems == 1 ? "produkt" : "produktów") + " - Pokaż szczegóły" :
                    totalItems + " " + (totalItems == 1 ? "produkt" : "produktów") + " - Ukryj szczegóły");
            });
            
            content.add(toggleItemsButton, itemsContainer);
        }
        
        // Wartość zamówienia
        Div totalSection = new Div();
        totalSection.addClassName("order-total-section");
        totalSection.getStyle().set("margin-top", "1rem");
        totalSection.getStyle().set("padding-top", "1rem");
        totalSection.getStyle().set("border-top", "1px solid var(--border-color)");
        
        Span totalLabel = new Span("Wartość zamówienia: ");
        totalLabel.getStyle().set("font-weight", "bold");
        
        Span totalValue = new Span(String.format("%.2f zł", order.totalPrice() != null ? order.totalPrice().doubleValue() : 0.0));
        totalValue.getStyle().set("font-weight", "bold");
        totalValue.getStyle().set("font-size", "1.1rem");
        totalValue.getStyle().set("color", "var(--accent-color)");
        
        totalSection.add(totalLabel, totalValue);
        content.add(totalSection);
        
        // Przycisk aktualizacji statusu dla aktywnych zamówień w statusie IN_DELIVERY
        if (isActive && "IN_DELIVERY".equalsIgnoreCase(order.status())) {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setWidthFull();
            actions.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.CENTER);
            actions.getStyle().set("margin-top", "1rem");
            
            Button deliverButton = new Button("Oznacz jako dostarczone");
            deliverButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
            deliverButton.addClickListener(e -> updateOrderStatus(order.id(), "DELIVERED"));
            
            actions.add(deliverButton);
            content.add(actions);
        }
        
        card.add(content);
        return card;
    }
    
    private void updateOrderStatus(Long orderId, String status) {
        try {
            getElement().executeJs(
                "const token = localStorage.getItem('eatgo-token'); " +
                "if (!token) { " +
                "  $0.$server.onError('Musisz być zalogowany'); " +
                "  return; " +
                "} " +
                "fetch('/api/courier/orders/' + $1 + '/status', { " +
                "  method: 'PUT', " +
                "  headers: { " +
                "    'Authorization': 'Bearer ' + token, " +
                "    'Content-Type': 'application/json' " +
                "  }, " +
                "  body: JSON.stringify({ status: $2 }) " +
                "}) " +
                ".then(r => { " +
                "  if (!r.ok) { " +
                "    return r.text().then(text => { " +
                "      throw new Error('HTTP ' + r.status + ': ' + (text || r.statusText)); " +
                "    }); " +
                "  } " +
                "  return r.json(); " +
                "}) " +
                ".then(updatedOrder => { " +
                "  $0.$server.onStatusUpdated('Status zamówienia został zaktualizowany'); " +
                "  setTimeout(function() { $0.$server.loadOrdersDelayed(); }, 500); " +
                "}) " +
                ".catch(e => { " +
                "  console.error('Error updating status:', e); " +
                "  $0.$server.onError('Błąd podczas aktualizacji statusu: ' + e.message); " +
                "});",
                getElement(), String.valueOf(orderId), status
            );
        } catch (Exception e) {
            System.err.println("CourierDashboard: Error in updateOrderStatus: " + e.getMessage());
            e.printStackTrace();
            getUI().ifPresent(ui -> {
                ui.access(() -> {
                    Notification.show("Błąd podczas aktualizacji statusu: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER);
                });
            });
        }
    }
    
    @com.vaadin.flow.component.ClientCallable
    public void onStatusUpdated(String message) {
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                Notification.show(message, 3000, Notification.Position.TOP_CENTER);
            });
        });
    }
    
    private String getStatusLabel(String status) {
        if (status == null) return "Nieznany";
        return switch (status.toUpperCase()) {
            case "PLACED" -> "Złożone";
            case "ACCEPTED" -> "Przyjęte";
            case "COOKING" -> "W przygotowaniu";
            case "READY" -> "Gotowe";
            case "IN_DELIVERY" -> "W dostawie";
            case "DELIVERED" -> "Dostarczone";
            case "CANCELLED" -> "Anulowane";
            default -> status;
        };
    }
    
    private String getStatusColor(String status) {
        if (status == null) return "var(--lumo-secondary-text-color)";
        return switch (status.toUpperCase()) {
            case "PLACED", "ACCEPTED" -> "var(--lumo-primary-color)";
            case "COOKING", "READY", "IN_DELIVERY" -> "var(--lumo-warning-text-color)";
            case "DELIVERED" -> "var(--lumo-success-text-color)";
            case "CANCELLED" -> "var(--lumo-error-text-color)";
            default -> "var(--lumo-secondary-text-color)";
        };
    }
    
    private String getStatusBackgroundColor(String status) {
        if (status == null) return "var(--lumo-contrast-5pct)";
        return switch (status.toUpperCase()) {
            case "PLACED", "ACCEPTED" -> "var(--lumo-primary-color-10pct)";
            case "COOKING", "READY", "IN_DELIVERY" -> "var(--lumo-warning-color-10pct)";
            case "DELIVERED" -> "var(--lumo-success-color-10pct)";
            case "CANCELLED" -> "var(--lumo-error-color-10pct)";
            default -> "var(--lumo-contrast-5pct)";
        };
    }
    
    private String formatDate(java.time.LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }
    
    private String formatDateFromOffset(java.time.OffsetDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }
    
    @com.vaadin.flow.component.ClientCallable
    public void onError(String errorMessage) {
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                Notification.show("Błąd: " + errorMessage, 3000, Notification.Position.TOP_CENTER);
            });
        });
    }
}
