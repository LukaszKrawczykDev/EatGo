package pollub.eatgo.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.fasterxml.jackson.databind.ObjectMapper;
import pollub.eatgo.dto.order.OrderDetailsDto;
import pollub.eatgo.dto.order.OrderItemDto;
import pollub.eatgo.service.AuthenticationService;
import pollub.eatgo.service.TokenValidationService;
import pollub.eatgo.views.components.HeaderComponent;

import java.util.List;

@Route("order")
@PageTitle("EatGo - Szczegóły zamówienia")
public class OrderDetailsView extends VerticalLayout implements HasUrlParameter<String> {
    
    private final AuthenticationService authService;
    private final TokenValidationService tokenValidationService;
    private final ObjectMapper objectMapper;
    private Long orderId;
    private Div contentContainer;
    
    public OrderDetailsView(AuthenticationService authService, TokenValidationService tokenValidationService) {
        this.authService = authService;
        this.tokenValidationService = tokenValidationService;
        
        // Inicjalizacja ObjectMapper z obsługą dat Java 8
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        this.objectMapper.disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        
        setSizeFull();
        setSpacing(false);
        setPadding(false);
        
        HeaderComponent headerComponent = new HeaderComponent(authService, tokenValidationService);
        add(headerComponent);
        
        Div content = new Div();
        content.addClassName("order-details-content");
        content.getStyle().set("max-width", "1200px");
        content.getStyle().set("margin", "0 auto");
        content.getStyle().set("padding", "2rem");
        
        contentContainer = new Div();
        contentContainer.setWidthFull();
        
        // Dodaj placeholder podczas ładowania
        Div loadingDiv = new Div();
        loadingDiv.setText("Ładowanie szczegółów zamówienia...");
        loadingDiv.getStyle().set("text-align", "center");
        loadingDiv.getStyle().set("padding", "2rem");
        loadingDiv.getStyle().set("color", "var(--lumo-secondary-text-color)");
        contentContainer.add(loadingDiv);
        
        content.add(contentContainer);
        add(content);
    }
    
    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        System.out.println("OrderDetailsView.setParameter called with: " + parameter);
        try {
            orderId = Long.parseLong(parameter);
            System.out.println("OrderDetailsView: Parsed orderId: " + orderId);
        } catch (NumberFormatException e) {
            System.err.println("OrderDetailsView: Invalid order ID format: " + parameter);
            Notification.show("Nieprawidłowy identyfikator zamówienia", 3000, Notification.Position.TOP_CENTER);
            event.forwardTo(OrdersView.class);
        }
    }
    
    @Override
    protected void onAttach(com.vaadin.flow.component.AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        System.out.println("OrderDetailsView.onAttach called, orderId: " + orderId);
        if (orderId != null) {
            // Opóźnij ładowanie, aby upewnić się, że komponent jest w pełni zrenderowany
            getUI().ifPresent(ui -> {
                ui.access(() -> {
                    System.out.println("OrderDetailsView: Scheduling loadOrderDetails");
                    ui.getPage().executeJs(
                        "console.log('OrderDetailsView: setTimeout callback executing'); " +
                        "setTimeout(function() { " +
                        "  console.log('OrderDetailsView: Calling loadOrderDetailsDelayed'); " +
                        "  $0.$server.loadOrderDetailsDelayed(); " +
                        "}, 200);",
                        getElement()
                    );
                });
            });
        }
    }
    
    @com.vaadin.flow.component.ClientCallable
    public void loadOrderDetailsDelayed() {
        System.out.println("OrderDetailsView.loadOrderDetailsDelayed called");
        loadOrderDetails();
    }
    
    private void loadOrderDetails() {
        System.out.println("OrderDetailsView.loadOrderDetails called for orderId: " + orderId);
        try {
            getUI().ifPresent(ui -> {
                ui.access(() -> {
                    getElement().executeJs(
                        "const token = localStorage.getItem('eatgo-token'); " +
                        "console.log('OrderDetailsView: Token exists:', !!token); " +
                        "if (!token || token === 'null' || token === '') { " +
                        "  console.error('OrderDetailsView: No token found'); " +
                        "  $0.$server.onError('Musisz być zalogowany, aby zobaczyć szczegóły zamówienia'); " +
                        "  return; " +
                        "} " +
                        "console.log('OrderDetailsView: Loading order details for ID:', $1); " +
                        "const url = '/api/orders/' + $1; " +
                        "console.log('OrderDetailsView: Fetching from URL:', url); " +
                        "fetch(url, { " +
                        "  method: 'GET', " +
                        "  headers: { " +
                        "    'Authorization': 'Bearer ' + token, " +
                        "    'Content-Type': 'application/json' " +
                        "  } " +
                        "}) " +
                        ".then(r => { " +
                        "  console.log('OrderDetailsView: Response status:', r.status, 'statusText:', r.statusText); " +
                        "  if (!r.ok) { " +
                        "    return r.text().then(text => { " +
                        "      console.error('OrderDetailsView: Error response body:', text); " +
                        "      throw new Error('HTTP ' + r.status + ': ' + (text || r.statusText)); " +
                        "    }); " +
                        "  } " +
                        "  return r.json(); " +
                        "}) " +
                        ".then(details => { " +
                        "  console.log('OrderDetailsView: Loaded order details successfully, details:', details); " +
                        "  const jsonStr = JSON.stringify(details); " +
                        "  console.log('OrderDetailsView: JSON string length:', jsonStr.length); " +
                        "  $0.$server.displayOrderDetails(jsonStr); " +
                        "}) " +
                        ".catch(e => { " +
                        "  console.error('OrderDetailsView: Error loading order details:', e); " +
                        "  console.error('OrderDetailsView: Error message:', e.message); " +
                        "  console.error('OrderDetailsView: Error stack:', e.stack); " +
                        "  $0.$server.onError('Błąd podczas ładowania szczegółów zamówienia: ' + e.message); " +
                        "});",
                        getElement(), String.valueOf(orderId)
                    );
                });
            });
        } catch (Exception e) {
            System.err.println("OrderDetailsView: Error in loadOrderDetails: " + e.getMessage());
            e.printStackTrace();
            getUI().ifPresent(ui -> {
                ui.access(() -> {
                    Notification.show("Błąd podczas ładowania szczegółów zamówienia: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER);
                    getUI().ifPresent(u -> u.navigate(OrdersView.class));
                });
            });
        }
    }
    
    @com.vaadin.flow.component.ClientCallable
    public void displayOrderDetails(String orderDetailsJson) {
        System.out.println("OrderDetailsView.displayOrderDetails called with JSON length: " + (orderDetailsJson != null ? orderDetailsJson.length() : 0));
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                try {
                    if (orderDetailsJson == null || orderDetailsJson.trim().isEmpty() || orderDetailsJson.equals("null")) {
                        System.err.println("OrderDetailsView: Empty or null JSON");
                        Notification.show("Brak danych zamówienia", 3000, Notification.Position.TOP_CENTER);
                        getUI().ifPresent(u -> u.navigate(OrdersView.class));
                        return;
                    }
                    
                    // Sprawdź czy to jest poprawny JSON
                    if (!orderDetailsJson.trim().startsWith("{")) {
                        System.err.println("OrderDetailsView: Invalid JSON format - doesn't start with '{': " + orderDetailsJson.substring(0, Math.min(100, orderDetailsJson.length())));
                        Notification.show("Błąd: Nieprawidłowy format danych", 3000, Notification.Position.TOP_CENTER);
                        getUI().ifPresent(u -> u.navigate(OrdersView.class));
                        return;
                    }
                    
                    if (contentContainer == null) {
                        System.err.println("OrderDetailsView: contentContainer is null!");
                        Notification.show("Błąd: Kontener nie został zainicjalizowany", 3000, Notification.Position.TOP_CENTER);
                        getUI().ifPresent(u -> u.navigate(OrdersView.class));
                        return;
                    }
                    
                    System.out.println("OrderDetailsView: Attempting to parse JSON...");
                    OrderDetailsDto orderDetails = objectMapper.readValue(orderDetailsJson, OrderDetailsDto.class);
                    System.out.println("OrderDetailsView: Parsed order details for order ID: " + orderDetails.id());
                    System.out.println("OrderDetailsView: Restaurant: " + (orderDetails.restaurant() != null ? orderDetails.restaurant().name() : "null"));
                    System.out.println("OrderDetailsView: Items count: " + (orderDetails.items() != null ? orderDetails.items().size() : 0));
                    
                    System.out.println("OrderDetailsView: Clearing contentContainer and adding new content...");
                    contentContainer.removeAll();
                    Div orderContent = createOrderDetailsContent(orderDetails);
                    System.out.println("OrderDetailsView: Created order content, adding to container...");
                    contentContainer.add(orderContent);
                    System.out.println("OrderDetailsView: Content added successfully");
                } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                    System.err.println("OrderDetailsView: JSON parsing error: " + e.getMessage());
                    e.printStackTrace();
                    Notification.show("Błąd podczas parsowania danych zamówienia: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER);
                    getUI().ifPresent(u -> u.navigate(OrdersView.class));
                } catch (Exception e) {
                    System.err.println("OrderDetailsView: Error displaying order details: " + e.getMessage());
                    e.printStackTrace();
                    Notification.show("Błąd podczas wyświetlania szczegółów zamówienia: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER);
                    getUI().ifPresent(u -> u.navigate(OrdersView.class));
                }
            });
        });
    }
    
    private Div createOrderDetailsContent(OrderDetailsDto orderDetails) {
        Div container = new Div();
        container.setWidthFull();
        
        // Przycisk powrotu
        Button backButton = new Button("← Powrót do zamówień", VaadinIcon.ARROW_LEFT.create());
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        backButton.addClickListener(e -> getUI().ifPresent(u -> u.navigate(OrdersView.class)));
        
        // Nagłówek
        H2 title = new H2("Szczegóły zamówienia #" + orderDetails.id());
        title.getStyle().set("margin-top", "1rem");
        
        // Status zamówienia
        Div statusDiv = new Div();
        statusDiv.getStyle().set("margin", "1rem 0");
        statusDiv.getStyle().set("padding", "1rem");
        statusDiv.getStyle().set("background", getStatusBackgroundColor(orderDetails.status()));
        statusDiv.getStyle().set("border-radius", "8px");
        Span statusLabel = new Span("Status: ");
        statusLabel.getStyle().set("font-weight", "bold");
        Span statusValue = new Span(getStatusLabel(orderDetails.status()));
        statusValue.getStyle().set("color", getStatusColor(orderDetails.status()));
        statusValue.getStyle().set("font-weight", "bold");
        statusValue.getStyle().set("font-size", "1.1rem");
        statusDiv.add(statusLabel, statusValue);
        
        // Informacje o restauracji
        String restaurantName = orderDetails.restaurant() != null && orderDetails.restaurant().name() != null ? 
            orderDetails.restaurant().name() : "Nieznana restauracja";
        Div restaurantDiv = createInfoSection("Restauracja", restaurantName);
        
        // Adres dostawy
        String fullAddress = "Nieznany";
        if (orderDetails.deliveryAddress() != null) {
            fullAddress = orderDetails.deliveryAddress().street() != null ? orderDetails.deliveryAddress().street() : "";
            if (orderDetails.deliveryAddress().apartmentNumber() != null && !orderDetails.deliveryAddress().apartmentNumber().isEmpty()) {
                fullAddress += "/" + orderDetails.deliveryAddress().apartmentNumber();
            }
            if (orderDetails.deliveryAddress().postalCode() != null) {
                fullAddress += ", " + orderDetails.deliveryAddress().postalCode();
            }
            if (orderDetails.deliveryAddress().city() != null) {
                fullAddress += " " + orderDetails.deliveryAddress().city();
            }
        }
        Div addressDiv = createInfoSection("Adres dostawy", fullAddress);
        
        // Data zamówienia
        String orderDate = orderDetails.createdAt() != null ? 
            orderDetails.createdAt().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : 
            "Nieznana";
        Div dateDiv = createInfoSection("Data zamówienia", orderDate);
        
        // Lista produktów
        Div itemsSection = new Div();
        itemsSection.getStyle().set("margin-top", "2rem");
        H3 itemsTitle = new H3("Produkty");
        itemsTitle.getStyle().set("margin-bottom", "1rem");
        itemsSection.add(itemsTitle);
        
        VerticalLayout itemsList = new VerticalLayout();
        itemsList.setSpacing(true);
        itemsList.setPadding(false);
        itemsList.setWidthFull();
        
        if (orderDetails.items() != null && !orderDetails.items().isEmpty()) {
            for (OrderItemDto item : orderDetails.items()) {
                itemsList.add(createItemCard(item));
            }
        } else {
            Div emptyMsg = new Div();
            emptyMsg.setText("Brak produktów");
            emptyMsg.getStyle().set("text-align", "center");
            emptyMsg.getStyle().set("padding", "2rem");
            emptyMsg.getStyle().set("color", "var(--lumo-secondary-text-color)");
            itemsList.add(emptyMsg);
        }
        
        itemsSection.add(itemsList);
        
        // Podsumowanie
        Div summarySection = new Div();
        summarySection.getStyle().set("margin-top", "2rem");
        summarySection.getStyle().set("padding", "1.5rem");
        summarySection.getStyle().set("background", "var(--lumo-contrast-5pct)");
        summarySection.getStyle().set("border-radius", "8px");
        
        HorizontalLayout subtotalLayout = new HorizontalLayout();
        subtotalLayout.setWidthFull();
        subtotalLayout.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);
        Span subtotalLabel = new Span("Suma pośrednia:");
        Span subtotalValue = new Span(String.format("%.2f zł", 
            orderDetails.totalPrice().doubleValue() - orderDetails.deliveryPrice().doubleValue()));
        subtotalLayout.add(subtotalLabel, subtotalValue);
        
        HorizontalLayout deliveryLayout = new HorizontalLayout();
        deliveryLayout.setWidthFull();
        deliveryLayout.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);
        Span deliveryLabel = new Span("Dostawa:");
        Span deliveryValue = new Span(String.format("%.2f zł", orderDetails.deliveryPrice().doubleValue()));
        deliveryLayout.add(deliveryLabel, deliveryValue);
        
        HorizontalLayout totalLayout = new HorizontalLayout();
        totalLayout.setWidthFull();
        totalLayout.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);
        totalLayout.getStyle().set("margin-top", "1rem");
        totalLayout.getStyle().set("padding-top", "1rem");
        totalLayout.getStyle().set("border-top", "2px solid var(--lumo-contrast-20pct)");
        Span totalLabel = new Span("Suma całkowita:");
        totalLabel.getStyle().set("font-weight", "bold");
        totalLabel.getStyle().set("font-size", "1.2rem");
        Span totalValue = new Span(String.format("%.2f zł", orderDetails.totalPrice().doubleValue()));
        totalValue.getStyle().set("font-weight", "bold");
        totalValue.getStyle().set("font-size", "1.2rem");
        totalValue.getStyle().set("color", "var(--lumo-primary-color)");
        totalLayout.add(totalLabel, totalValue);
        
        summarySection.add(subtotalLayout, deliveryLayout, totalLayout);
        
        container.add(backButton, title, statusDiv, restaurantDiv, addressDiv, dateDiv, itemsSection, summarySection);
        
        return container;
    }
    
    
    private Div createInfoSection(String label, String value) {
        Div section = new Div();
        section.getStyle().set("margin", "0.5rem 0");
        Span labelSpan = new Span(label + ": ");
        labelSpan.getStyle().set("font-weight", "bold");
        Span valueSpan = new Span(value);
        section.add(labelSpan, valueSpan);
        return section;
    }
    
    private Div createItemCard(OrderItemDto item) {
        Div card = new Div();
        card.getStyle().set("padding", "1rem");
        card.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        card.getStyle().set("border-radius", "8px");
        card.getStyle().set("background", "var(--lumo-base-color)");
        
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);
        layout.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        
        Div itemInfo = new Div();
        Span itemName = new Span(item.dishName() != null ? item.dishName() : "Nieznane danie");
        itemName.getStyle().set("font-weight", "bold");
        itemName.getStyle().set("display", "block");
        Span itemQuantity = new Span("Ilość: " + item.quantity());
        itemQuantity.getStyle().set("color", "var(--lumo-secondary-text-color)");
        itemQuantity.getStyle().set("display", "block");
        itemQuantity.getStyle().set("margin-top", "0.5rem");
        itemInfo.add(itemName, itemQuantity);
        
        Span itemPrice = new Span(String.format("%.2f zł", item.priceSnapshot() * item.quantity()));
        itemPrice.getStyle().set("font-weight", "bold");
        itemPrice.getStyle().set("font-size", "1.1rem");
        
        layout.add(itemInfo, itemPrice);
        card.add(layout);
        
        return card;
    }
    
    private String getStatusLabel(String status) {
        if (status == null) return "Nieznany";
        return switch (status.toUpperCase()) {
            case "PLACED" -> "Złożone";
            case "ACCEPTED" -> "Przyjęte";
            case "COOKING" -> "W przygotowaniu";
            case "READY" -> "Gotowe";
            case "IN_DELIVERY" -> "W drodze";
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
    
    @com.vaadin.flow.component.ClientCallable
    public void onError(String errorMessage) {
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                Notification.show("Błąd: " + errorMessage, 3000, Notification.Position.TOP_CENTER);
                getUI().ifPresent(u -> u.navigate(OrdersView.class));
            });
        });
    }
}

