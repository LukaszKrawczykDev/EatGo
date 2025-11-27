package pollub.eatgo.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.fasterxml.jackson.databind.ObjectMapper;
import pollub.eatgo.dto.order.OrderDto;
import pollub.eatgo.service.AuthenticationService;
import pollub.eatgo.service.OrderNotificationService;
import pollub.eatgo.service.TokenValidationService;
import pollub.eatgo.views.components.HeaderComponent;

import java.util.ArrayList;
import java.util.List;

@Route("orders")
@PageTitle("EatGo - Moje zamówienia")
public class OrdersView extends VerticalLayout {
    
    private final AuthenticationService authService;
    private final TokenValidationService tokenValidationService;
    private final OrderNotificationService orderNotificationService;
    private final ObjectMapper objectMapper;
    private VerticalLayout activeOrdersContainer;
    private VerticalLayout completedOrdersContainer;
    private java.util.Map<Long, String> lastStatuses = new java.util.HashMap<>();
    private java.util.Set<Long> shownDeliveredDialogs = new java.util.HashSet<>();
    private Long currentUserId;
    private com.vaadin.flow.component.dialog.Dialog deliveredDialog;
    
    public OrdersView(AuthenticationService authService,
                      TokenValidationService tokenValidationService,
                      OrderNotificationService orderNotificationService) {
        this.authService = authService;
        this.tokenValidationService = tokenValidationService;
        this.orderNotificationService = orderNotificationService;

        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        this.objectMapper.disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        
        setSizeFull();
        setSpacing(false);
        setPadding(false);
        
        HeaderComponent headerComponent = new HeaderComponent(authService, tokenValidationService, orderNotificationService);
        add(headerComponent);
        
        Div content = new Div();
        content.addClassName("orders-content");
        content.getStyle().set("max-width", "1200px");
        content.getStyle().set("margin", "0 auto");
        content.getStyle().set("padding", "2rem");
        
        H2 title = new H2("Moje zamówienia");
        title.addClassName("orders-title");

        Div activeSection = new Div();
        activeSection.addClassName("orders-section");
        H3 activeTitle = new H3("W realizacji");
        activeTitle.addClassName("orders-section-title");
        activeOrdersContainer = new VerticalLayout();
        activeOrdersContainer.setSpacing(true);
        activeOrdersContainer.setPadding(false);
        activeOrdersContainer.setWidthFull();
        activeSection.add(activeTitle, activeOrdersContainer);

        Div completedSection = new Div();
        completedSection.addClassName("orders-section");
        completedSection.getStyle().set("margin-top", "3rem");
        H3 completedTitle = new H3("Zakończone");
        completedTitle.addClassName("orders-section-title");
        completedOrdersContainer = new VerticalLayout();
        completedOrdersContainer.setSpacing(true);
        completedOrdersContainer.setPadding(false);
        completedOrdersContainer.setWidthFull();
        completedSection.add(completedTitle, completedOrdersContainer);
        
        content.add(title, activeSection, completedSection);
        add(content);
    }
    
    @Override
    protected void onAttach(com.vaadin.flow.component.AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                ui.getPage().executeJs(
                    "(function(el){" +
                    "  const uid = localStorage.getItem('eatgo-userId');" +
                    "  el.$server.onUserIdResolved(uid || '');" +
                    "  setTimeout(function(){ el.$server.loadOrdersDelayed(); }, 100);" +
                    "})(arguments[0]);",
                    getElement()
                );
            });
        });
    }
    
    @com.vaadin.flow.component.ClientCallable
    public void onUserIdResolved(String userId) {
        if (userId == null || userId.isBlank()) {
            this.currentUserId = null;
            return;
        }
        try {
            this.currentUserId = Long.parseLong(userId);
        } catch (NumberFormatException e) {
            this.currentUserId = null;
        }
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
                "fetch('/api/orders', { " +
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
                "  console.log('OrdersView: Loaded ' + orders.length + ' orders'); " +
                "  $0.$server.displayOrders(JSON.stringify(orders)); " +
                "}) " +
                ".catch(e => { " +
                "  console.error('Error loading orders:', e); " +
                "  $0.$server.onError('Błąd podczas ładowania zamówień: ' + e.message); " +
                "});",
                getElement()
            );
        } catch (Exception e) {
            System.err.println("OrdersView: Error in loadOrders: " + e.getMessage());
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
        System.out.println("OrdersView.displayOrders called with JSON length: " + (ordersJson != null ? ordersJson.length() : 0));
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                try {
                    if (ordersJson == null || ordersJson.trim().isEmpty() || ordersJson.equals("null")) {
                        System.out.println("OrdersView: Empty or null JSON, showing empty state");
                        showEmptyState();
                        return;
                    }

                    if (!ordersJson.trim().startsWith("[")) {
                        System.err.println("OrdersView: Invalid JSON format - doesn't start with '[': " + ordersJson.substring(0, Math.min(100, ordersJson.length())));
                        Notification.show("Błąd: Nieprawidłowy format danych", 3000, Notification.Position.TOP_CENTER);
                        showEmptyState();
                        return;
                    }
                    
                    List<OrderDto> orders = objectMapper.readValue(ordersJson, 
                        objectMapper.getTypeFactory().constructCollectionType(List.class, OrderDto.class));
                    
                    System.out.println("OrdersView: Parsed " + orders.size() + " orders");

                    boolean isFirstLoad = lastStatuses.isEmpty();
                    
                    for (OrderDto order : orders) {
                        String previous = lastStatuses.get(order.id());
                        String current = order.status();

                        if (!isFirstLoad 
                                && previous != null
                                && !"DELIVERED".equalsIgnoreCase(previous)
                                && "DELIVERED".equalsIgnoreCase(current)
                                && !shownDeliveredDialogs.contains(order.id())) {
                            System.out.println("OrdersView: Detected status change to DELIVERED for order #" + order.id());
                            shownDeliveredDialogs.add(order.id());
                            showDeliveredDialog(order.id());
                        }

                        lastStatuses.put(order.id(), current);
                    }
                    
                    if (activeOrdersContainer == null || completedOrdersContainer == null) {
                        System.err.println("OrdersView: Containers are null!");
                        return;
                    }
                    
                    activeOrdersContainer.removeAll();
                    completedOrdersContainer.removeAll();
                    
                    List<OrderDto> activeOrders = new ArrayList<>();
                    List<OrderDto> completedOrders = new ArrayList<>();
                    
                    for (OrderDto order : orders) {
                        if (isCompleted(order.status())) {
                            completedOrders.add(order);
                        } else {
                            activeOrders.add(order);
                        }
                    }
                    
                    if (activeOrders.isEmpty()) {
                        Div emptyMsg = new Div();
                        emptyMsg.setText("Brak zamówień w realizacji");
                        emptyMsg.getStyle().set("text-align", "center");
                        emptyMsg.getStyle().set("padding", "2rem");
                        emptyMsg.getStyle().set("color", "var(--lumo-secondary-text-color)");
                        activeOrdersContainer.add(emptyMsg);
                    } else {
                        for (OrderDto order : activeOrders) {
                            activeOrdersContainer.add(createOrderCard(order));
                        }
                    }
                    
                    if (completedOrders.isEmpty()) {
                        Div emptyMsg = new Div();
                        emptyMsg.setText("Brak zakończonych zamówień");
                        emptyMsg.getStyle().set("text-align", "center");
                        emptyMsg.getStyle().set("padding", "2rem");
                        emptyMsg.getStyle().set("color", "var(--lumo-secondary-text-color)");
                        completedOrdersContainer.add(emptyMsg);
                    } else {
                        for (OrderDto order : completedOrders) {
                            completedOrdersContainer.add(createOrderCard(order));
                        }
                    }
                } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                    System.err.println("OrdersView: JSON parsing error: " + e.getMessage());
                    e.printStackTrace();
                    Notification.show("Błąd podczas parsowania danych zamówień: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER);
                    showEmptyState();
                } catch (Exception e) {
                    System.err.println("OrdersView: Error displaying orders: " + e.getMessage());
                    e.printStackTrace();
                    Notification.show("Błąd podczas wyświetlania zamówień: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER);
                    showEmptyState();
                }
            });
        });
    }
    
    private void showEmptyState() {
        activeOrdersContainer.removeAll();
        completedOrdersContainer.removeAll();
        
        Div emptyMsg = new Div();
        emptyMsg.setText("Brak zamówień");
        emptyMsg.getStyle().set("text-align", "center");
        emptyMsg.getStyle().set("padding", "2rem");
        emptyMsg.getStyle().set("color", "var(--lumo-secondary-text-color)");
        activeOrdersContainer.add(emptyMsg);
    }
    
    private boolean isCompleted(String status) {
        if (status == null) return false;
        return status.equalsIgnoreCase("DELIVERED") || status.equalsIgnoreCase("CANCELLED");
    }
    
    private Div createOrderCard(OrderDto order) {
        Div card = new Div();
        card.addClassName("order-card");
        card.getStyle().set("padding", "1.5rem");
        card.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        card.getStyle().set("border-radius", "8px");
        card.getStyle().set("background", "var(--lumo-base-color)");
        card.getStyle().set("margin-bottom", "1rem");
        
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(false);
        content.setPadding(false);
        
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        
        Div orderInfo = new Div();
        Span orderNumber = new Span("#" + order.id());
        orderNumber.getStyle().set("font-weight", "bold");
        orderNumber.getStyle().set("font-size", "1.1rem");
        
        Span orderDate = new Span(formatDate(order.createdAt()));
        orderDate.getStyle().set("color", "var(--lumo-secondary-text-color)");
        orderDate.getStyle().set("font-size", "0.875rem");
        orderDate.getStyle().set("margin-left", "1rem");
        
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
        
        Div itemsInfo = new Div();
        itemsInfo.getStyle().set("margin-top", "1rem");
        itemsInfo.getStyle().set("color", "var(--lumo-secondary-text-color)");
        int totalItems = order.items() != null ? order.items().stream()
            .mapToInt(item -> item.quantity())
            .sum() : 0;
        itemsInfo.setText(totalItems + " " + (totalItems == 1 ? "produkt" : "produktów"));
        
        HorizontalLayout totalLayout = new HorizontalLayout();
        totalLayout.setWidthFull();
        totalLayout.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);
        totalLayout.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        totalLayout.getStyle().set("margin-top", "1rem");
        totalLayout.getStyle().set("padding-top", "1rem");
        totalLayout.getStyle().set("border-top", "1px solid var(--lumo-contrast-10pct)");
        
        Span totalLabel = new Span("Wartość zamówienia:");
        totalLabel.getStyle().set("font-weight", "bold");
        Span totalValue = new Span(String.format("%.2f zł", order.totalPrice()));
        totalValue.getStyle().set("font-weight", "bold");
        totalValue.getStyle().set("font-size", "1.1rem");
        totalValue.getStyle().set("color", "var(--lumo-primary-color)");
        
        totalLayout.add(totalLabel, totalValue);
        
        Button detailsBtn = new Button("Szczegóły", VaadinIcon.EYE.create());
        detailsBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        detailsBtn.addClickListener(e -> {
            getUI().ifPresent(u -> u.navigate("order/" + order.id()));
        });
        
        content.add(header, itemsInfo, totalLayout, detailsBtn);
        card.add(content);
        
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
    
    private String formatDate(java.time.LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }

    private void showDeliveredDialog(Long orderId) {
        if (deliveredDialog == null) {
            deliveredDialog = new com.vaadin.flow.component.dialog.Dialog();
            deliveredDialog.setModal(true);
            deliveredDialog.setDraggable(false);
            deliveredDialog.setResizable(false);
        } else {
            deliveredDialog.removeAll();
        }

        com.vaadin.flow.component.html.H3 title = new com.vaadin.flow.component.html.H3(
                "Zamówienie nr " + orderId + " zostało dostarczone");
        com.vaadin.flow.component.html.Paragraph info = new com.vaadin.flow.component.html.Paragraph(
                "Dziękujemy za skorzystanie z EatGo!");

        com.vaadin.flow.component.button.Button ok = new com.vaadin.flow.component.button.Button("OK", e -> {
            deliveredDialog.close();
            if (currentUserId != null) {
                orderNotificationService.clearForUser(currentUserId);
            }
        });
        ok.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY);

        com.vaadin.flow.component.orderedlayout.VerticalLayout layout =
                new com.vaadin.flow.component.orderedlayout.VerticalLayout(title, info, ok);
        layout.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        layout.setPadding(true);
        layout.setSpacing(true);

        deliveredDialog.add(layout);
        deliveredDialog.open();
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

