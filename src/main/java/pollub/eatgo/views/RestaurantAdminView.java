package pollub.eatgo.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import pollub.eatgo.dto.courier.CourierCreateDto;
import pollub.eatgo.dto.courier.CourierDto;
import pollub.eatgo.dto.courier.CourierUpdateDto;
import pollub.eatgo.dto.dish.DishCreateDto;
import pollub.eatgo.dto.dish.DishDto;
import pollub.eatgo.dto.dish.DishUpdateDto;
import pollub.eatgo.dto.order.OrderDto;
import pollub.eatgo.dto.restaurant.RestaurantDto;
import pollub.eatgo.service.AuthenticationService;
import pollub.eatgo.service.OrderNotificationService;
import pollub.eatgo.service.PdfService;
import pollub.eatgo.service.RestaurantService;
import pollub.eatgo.service.ReviewService;
import pollub.eatgo.service.TokenValidationService;
import pollub.eatgo.views.components.HeaderComponent;

import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Route("restaurant")
@PageTitle("EatGo - Panel Restauracji")
public class RestaurantAdminView extends VerticalLayout implements BeforeEnterObserver {
    
    private final AuthenticationService authService;
    private final RestaurantService restaurantService;
    private final TokenValidationService tokenValidationService;
    private final PdfService pdfService;
    
    private Tabs tabs;
    private Div contentContainer;
    
    // Tabs
    private Tab ordersTab;
    private Tab dishesTab;
    private Tab couriersTab;
    private Tab statisticsTab;
    
    // Data
    private List<OrderDto> orders = new ArrayList<>();
    private List<DishDto> dishes = new ArrayList<>();
    private List<CourierDto> couriers = new ArrayList<>();
    private RestaurantDto restaurant;
    private String adminEmail;
    
    // Grids
    private Grid<OrderDto> ordersGrid;
    private Grid<DishDto> dishesGrid;
    private Grid<CourierDto> couriersGrid;
    
    public RestaurantAdminView(AuthenticationService authService, 
                               RestaurantService restaurantService,
                               TokenValidationService tokenValidationService,
                               PdfService pdfService,
                               ReviewService reviewService,
                               OrderNotificationService orderNotificationService) {
        this.authService = authService;
        this.restaurantService = restaurantService;
        this.tokenValidationService = tokenValidationService;
        this.pdfService = pdfService;
        
        setSizeFull();
        setSpacing(false);
        setPadding(false);
        addClassName("restaurant-admin-view");
        
        HeaderComponent header = new HeaderComponent(authService, tokenValidationService, orderNotificationService);
        add(header);
        
        createTabs();
        createContentContainer();
        
        add(tabs, contentContainer);
        
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
            showOrdersTab();
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
        // Try to get from SecurityContext as fallback
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
    
    private void createTabs() {
        tabs = new Tabs();
        tabs.setWidthFull();
        tabs.addClassName("restaurant-admin-tabs");
        
        ordersTab = new Tab();
        ordersTab.add(VaadinIcon.CLIPBOARD_TEXT.create(), new Span("Zamówienia"));
        
        dishesTab = new Tab();
        dishesTab.add(VaadinIcon.CUTLERY.create(), new Span("Dania"));
        
        couriersTab = new Tab();
        couriersTab.add(VaadinIcon.TRUCK.create(), new Span("Kurierzy"));

        statisticsTab = new Tab();
        statisticsTab.add(VaadinIcon.CHART_LINE.create(), new Span("Statystyki"));
        
        tabs.add(ordersTab, dishesTab, couriersTab, statisticsTab);
        
        tabs.addSelectedChangeListener(e -> {
            Tab selected = e.getSelectedTab();
            if (selected == ordersTab) {
                showOrdersTab();
            } else if (selected == dishesTab) {
                showDishesTab();
            } else if (selected == couriersTab) {
                showCouriersTab();
            } else if (selected == statisticsTab) {
                showStatisticsTab();
            }
        });
    }
    
    private void createContentContainer() {
        contentContainer = new Div();
        contentContainer.addClassName("restaurant-admin-content");
        contentContainer.setSizeFull();
        setFlexGrow(1, contentContainer);
    }
    
    private void loadRestaurantData() {
        try {
            String email = getAdminEmail();
            if (email == null) {
                Notification.show("Błąd autoryzacji", 5000, Notification.Position.TOP_CENTER);
                return;
            }
            orders = restaurantService.listOrders(email);
            dishes = restaurantService.getAllDishesForAdmin(email);
            couriers = restaurantService.listCouriers(email);
            restaurant = restaurantService.getRestaurantForAdmin(email);
        } catch (Exception e) {
            Notification.show("Błąd podczas ładowania danych: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER);
        }
    }
    
    private void showOrdersTab() {
        contentContainer.removeAll();
        
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setPadding(true);
        layout.setSizeFull();
        
        // Statistics cards
        Div statsContainer = createStatisticsCards();
        layout.add(statsContainer);
        
        H2 title = new H2("Zamówienia");
        
        // Filter ComboBox
        ComboBox<String> filterCombo = new ComboBox<>("Filtruj status");
        filterCombo.setItems("Wszystkie", "W toku", "Zakończone", "Anulowane");
        filterCombo.setValue("Wszystkie");
        filterCombo.setWidth("200px");
        
        Button exportBtn = new Button("Eksportuj Excel", VaadinIcon.DOWNLOAD.create(), e -> exportOrdersToExcel());
        exportBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        
        Button refreshBtn = new Button("Odśwież", VaadinIcon.REFRESH.create(), e -> {
            loadRestaurantData();
            showOrdersTab();
        });
        
        HorizontalLayout buttonsLayout = new HorizontalLayout(exportBtn, refreshBtn);
        buttonsLayout.setSpacing(true);
        
        HorizontalLayout header = new HorizontalLayout(title, filterCombo, buttonsLayout);
        header.setWidthFull();
        header.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        
        ordersGrid = new Grid<>(OrderDto.class, false);
        ordersGrid.addColumn(OrderDto::id).setHeader("ID").setAutoWidth(true).setSortable(true);
        ordersGrid.addColumn(new ComponentRenderer<>(order -> {
            Span statusBadge = new Span(getStatusLabel(order.status()));
            statusBadge.addClassName("order-status-badge");
            statusBadge.getStyle().set("padding", "0.25rem 0.75rem");
            statusBadge.getStyle().set("border-radius", "12px");
            statusBadge.getStyle().set("font-size", "0.875rem");
            statusBadge.getStyle().set("font-weight", "500");
            statusBadge.getStyle().set("color", getStatusColor(order.status()));
            statusBadge.getStyle().set("background-color", getStatusBackgroundColor(order.status()));
            return statusBadge;
        })).setHeader("Status").setAutoWidth(true);
        ordersGrid.addColumn(o -> String.format("%.2f zł", o.totalPrice())).setHeader("Wartość").setAutoWidth(true).setSortable(true);
        ordersGrid.addColumn(OrderDto::userEmail).setHeader("Klient").setAutoWidth(true).setSortable(true);
        ordersGrid.addColumn(o -> o.courierFullName() != null ? o.courierFullName() : (o.courierEmail() != null ? o.courierEmail() : "-"))
                  .setHeader("Kurier").setAutoWidth(true).setSortable(true);
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        ordersGrid.addColumn(o -> o.createdAt() != null ? o.createdAt().format(formatter) : "")
                  .setHeader("Data").setAutoWidth(true).setSortable(true);
        
        ordersGrid.addColumn(new ComponentRenderer<>(order -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(true);
            
            if ("PLACED".equals(order.status())) {
                Button acceptBtn = new Button("Przyjmij", e -> updateOrderStatus(order.id(), "ACCEPTED"));
                acceptBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
                actions.add(acceptBtn);
            } else if ("ACCEPTED".equals(order.status())) {
                Button prepareBtn = new Button("Przygotuj", e -> updateOrderStatus(order.id(), "COOKING"));
                prepareBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                actions.add(prepareBtn);
            } else if ("COOKING".equals(order.status())) {
                Button readyBtn = new Button("Gotowe", e -> updateOrderStatus(order.id(), "READY"));
                readyBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
                actions.add(readyBtn);
            } else if ("READY".equals(order.status()) && order.courierId() == null) {
                Button assignCourierBtn = new Button("Przypisz kuriera", e -> showAssignCourierDialog(order));
                assignCourierBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                actions.add(assignCourierBtn);
            } else if ("READY".equals(order.status()) && order.courierId() != null) {
                Button sendBtn = new Button("Wyślij", e -> updateOrderStatus(order.id(), "IN_DELIVERY"));
                sendBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
                actions.add(sendBtn);
            }
            
            Button detailsBtn = new Button("Szczegóły", e -> showOrderDetails(order));
            Button printBtn = new Button("Drukuj bon", VaadinIcon.PRINT.create(), e -> printReceipt(order));
            printBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            actions.add(detailsBtn, printBtn);
            
            return actions;
        })).setHeader("Akcje").setAutoWidth(true);
        
        // Apply filter
        filterCombo.addValueChangeListener(e -> {
            String filter = e.getValue();
            List<OrderDto> filtered = orders;
            if (filter != null) {
                switch (filter) {
                    case "W toku":
                        filtered = orders.stream()
                            .filter(o -> !"DELIVERED".equals(o.status()) && !"CANCELLED".equals(o.status()))
                            .collect(Collectors.toList());
                        break;
                    case "Zakończone":
                        filtered = orders.stream()
                            .filter(o -> "DELIVERED".equals(o.status()))
                            .collect(Collectors.toList());
                        break;
                    case "Anulowane":
                        filtered = orders.stream()
                            .filter(o -> "CANCELLED".equals(o.status()))
                            .collect(Collectors.toList());
                        break;
                }
            }
            ordersGrid.setItems(filtered);
        });
        
        ordersGrid.setItems(orders);
        ordersGrid.setSizeFull();
        
        layout.add(header, ordersGrid);
        layout.setFlexGrow(1, ordersGrid);
        
        contentContainer.add(layout);
    }
    
    private Div createStatisticsCards() {
        Div statsContainer = new Div();
        statsContainer.addClassName("stats-container");
        statsContainer.getStyle().set("display", "flex");
        statsContainer.getStyle().set("flex-direction", "row");
        statsContainer.getStyle().set("flex-wrap", "wrap");
        statsContainer.getStyle().set("gap", "1rem");
        statsContainer.getStyle().set("margin-bottom", "2rem");
        
        try {
            String email = getAdminEmail();
            if (email != null) {
                double todayRevenue = restaurantService.getTodayRevenue(email);
                long todayOrders = restaurantService.getTodayOrdersCount(email);
                long activeOrders = restaurantService.getActiveOrdersCount(email);
                Map<String, Integer> topDishes = restaurantService.getTopDishes(email, 3);
                
                statsContainer.add(createStatCard("Dzisiejszy przychód", String.format("%.2f zł", todayRevenue), VaadinIcon.MONEY));
                statsContainer.add(createStatCard("Zamówienia dziś", String.valueOf(todayOrders), VaadinIcon.CLIPBOARD_TEXT));
                statsContainer.add(createStatCard("W realizacji", String.valueOf(activeOrders), VaadinIcon.CLOCK));
                
                if (!topDishes.isEmpty()) {
                    String topDish = topDishes.entrySet().iterator().next().getKey();
                    statsContainer.add(createStatCard("Najpopularniejsze danie", topDish, VaadinIcon.THUMBS_UP));
                }
            }
        } catch (Exception e) {
            // Ignore errors in stats
        }
        
        return statsContainer;
    }
    
    private Div createStatCard(String title, String value, VaadinIcon icon) {
        Div card = new Div();
        card.addClassName("stat-card");
        card.getStyle().set("padding", "0.75rem 1rem");
        card.getStyle().set("border-radius", "8px");
        card.getStyle().set("background", "var(--bg-primary)");
        card.getStyle().set("border", "1px solid var(--border-color)");
        card.getStyle().set("box-shadow", "var(--shadow-sm)");
        card.getStyle().set("transition", "transform 0.2s, box-shadow 0.2s");
        card.getStyle().set("display", "flex");
        card.getStyle().set("flex-direction", "row");
        card.getStyle().set("align-items", "center");
        card.getStyle().set("justify-content", "space-between");
        
        HorizontalLayout leftSection = new HorizontalLayout();
        leftSection.setSpacing(true);
        leftSection.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        leftSection.setPadding(false);
        leftSection.getStyle().set("margin", "0");
        
        com.vaadin.flow.component.html.Span iconSpan = new com.vaadin.flow.component.html.Span();
        iconSpan.getElement().appendChild(icon.create().getElement());
        iconSpan.getStyle().set("font-size", "1.2rem");
        iconSpan.getStyle().set("opacity", "0.8");
        
        Span titleSpan = new Span(title);
        titleSpan.getStyle().set("font-size", "0.875rem");
        titleSpan.getStyle().set("color", "var(--text-secondary)");
        titleSpan.getStyle().set("font-weight", "500");
        titleSpan.getStyle().set("white-space", "nowrap");
        
        leftSection.add(iconSpan, titleSpan);
        
        Span valueSpan = new Span(value);
        valueSpan.getStyle().set("font-size", "1.25rem");
        valueSpan.getStyle().set("font-weight", "bold");
        valueSpan.getStyle().set("color", "var(--text-primary)");
        valueSpan.getStyle().set("white-space", "nowrap");
        valueSpan.getStyle().set("margin-left", "auto");
        
        card.add(leftSection, valueSpan);
        
        return card;
    }
    
    private void showDishesTab() {
        contentContainer.removeAll();
        
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setPadding(true);
        layout.setSizeFull();
        
        H2 title = new H2("Dania");
        Button addBtn = new Button("Dodaj danie", VaadinIcon.PLUS.create(), e -> showDishDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button refreshBtn = new Button("Odśwież", VaadinIcon.REFRESH.create(), e -> {
            loadRestaurantData();
            showDishesTab();
        });
        
        // Search and filter for dishes
        TextField dishSearchField = new TextField();
        dishSearchField.setPlaceholder("Szukaj dania...");
        dishSearchField.setWidth("250px");
        dishSearchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        
        ComboBox<String> categoryFilter = new ComboBox<>("Kategoria");
        categoryFilter.setItems("Wszystkie", "PIZZA", "BURGER", "SUSHI", "KEBAB", "MEXICAN", "ASIAN", "ITALIAN");
        categoryFilter.setValue("Wszystkie");
        categoryFilter.setWidth("150px");
        
        ComboBox<String> availabilityFilter = new ComboBox<>("Dostępność");
        availabilityFilter.setItems("Wszystkie", "Dostępne", "Niedostępne");
        availabilityFilter.setValue("Wszystkie");
        availabilityFilter.setWidth("150px");
        
        HorizontalLayout header = new HorizontalLayout(title, dishSearchField, categoryFilter, availabilityFilter, addBtn, refreshBtn);
        header.setWidthFull();
        header.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        header.getStyle().set("flex-wrap", "wrap");
        
        dishesGrid = new Grid<>(DishDto.class, false);
        dishesGrid.addColumn(new ComponentRenderer<>(dish -> {
            HorizontalLayout nameLayout = new HorizontalLayout();
            nameLayout.setSpacing(true);
            nameLayout.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
            nameLayout.setPadding(false);
            
            if (dish.imageUrl() != null && !dish.imageUrl().isEmpty()) {
                Image dishImage = new Image(dish.imageUrl(), dish.name());
                dishImage.getStyle().set("width", "40px");
                dishImage.getStyle().set("height", "40px");
                dishImage.getStyle().set("object-fit", "cover");
                dishImage.getStyle().set("border-radius", "4px");
                dishImage.getStyle().set("flex-shrink", "0");
                nameLayout.add(dishImage);
            }
            
            Span nameSpan = new Span(dish.name());
            nameLayout.add(nameSpan);
            nameLayout.setFlexGrow(1, nameSpan);
            
            return nameLayout;
        })).setHeader("Nazwa").setAutoWidth(true).setSortable(true);
        dishesGrid.addColumn(DishDto::description).setHeader("Opis").setAutoWidth(true);
        dishesGrid.addColumn(d -> String.format("%.2f zł", d.price())).setHeader("Cena").setAutoWidth(true).setSortable(true);
        dishesGrid.addColumn(d -> d.available() ? "Dostępne" : "Niedostępne").setHeader("Status").setAutoWidth(true).setSortable(true);
        dishesGrid.addColumn(DishDto::category).setHeader("Kategoria").setAutoWidth(true).setSortable(true);
        
        dishesGrid.addColumn(new ComponentRenderer<>(dish -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(true);
            
            Button editBtn = new Button("Edytuj", e -> showDishDialog(dish));
            Button deleteBtn = new Button("Usuń", e -> confirmDeleteDish(dish));
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            
            actions.add(editBtn, deleteBtn);
            return actions;
        })).setHeader("Akcje").setAutoWidth(true);
        
        // Apply filters for dishes
        Runnable updateDishesGrid = () -> {
            String search = dishSearchField.getValue();
            String category = categoryFilter.getValue();
            String availability = availabilityFilter.getValue();
            
            List<DishDto> filtered = dishes;
            
            if (search != null && !search.trim().isEmpty()) {
                String searchLower = search.toLowerCase();
                filtered = filtered.stream()
                    .filter(d -> d.name().toLowerCase().contains(searchLower) ||
                               (d.description() != null && d.description().toLowerCase().contains(searchLower)))
                    .collect(Collectors.toList());
            }
            
            if (category != null && !"Wszystkie".equals(category)) {
                filtered = filtered.stream()
                    .filter(d -> category.equals(d.category()))
                    .collect(Collectors.toList());
            }
            
            if (availability != null && !"Wszystkie".equals(availability)) {
                boolean available = "Dostępne".equals(availability);
                filtered = filtered.stream()
                    .filter(d -> d.available() == available)
                    .collect(Collectors.toList());
            }
            
            dishesGrid.setItems(filtered);
        };
        
        dishSearchField.addValueChangeListener(e -> updateDishesGrid.run());
        categoryFilter.addValueChangeListener(e -> updateDishesGrid.run());
        availabilityFilter.addValueChangeListener(e -> updateDishesGrid.run());
        
        dishesGrid.setItems(dishes);
        dishesGrid.setSizeFull();
        
        layout.add(header, dishesGrid);
        layout.setFlexGrow(1, dishesGrid);
        
        contentContainer.add(layout);
    }
    
    private void showCouriersTab() {
        contentContainer.removeAll();
        
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setPadding(true);
        layout.setSizeFull();
        
        H2 title = new H2("Kurierzy");
        Button addBtn = new Button("Dodaj kuriera", VaadinIcon.PLUS.create(), e -> showCourierDialog());
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button refreshBtn = new Button("Odśwież", VaadinIcon.REFRESH.create(), e -> {
            loadRestaurantData();
            showCouriersTab();
        });
        
        // Filter ComboBox
        ComboBox<String> filterCombo = new ComboBox<>("Filtruj status");
        filterCombo.setItems("Wszyscy", "Dostępni", "W trakcie dostawy");
        filterCombo.setValue("Wszyscy");
        filterCombo.setWidth("200px");
        
        HorizontalLayout header = new HorizontalLayout(title, filterCombo, addBtn, refreshBtn);
        header.setWidthFull();
        header.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        
        couriersGrid = new Grid<>(CourierDto.class, false);
        couriersGrid.addColumn(CourierDto::email).setHeader("Email").setAutoWidth(true);
        couriersGrid.addColumn(CourierDto::fullName).setHeader("Imię i nazwisko").setAutoWidth(true);
        couriersGrid.addColumn(new ComponentRenderer<>(courier -> {
            // Policz aktywne dostawy dla kuriera
            long activeDeliveries = orders.stream()
                .filter(o -> o.courierId() != null && o.courierId().equals(courier.id()) && "IN_DELIVERY".equals(o.status()))
                .count();
            
            String statusText = activeDeliveries == 0 ? "Dostępny" : activeDeliveries + " aktywnych dostaw";
            Span statusBadge = new Span(statusText);
            statusBadge.addClassName("courier-status-badge");
            statusBadge.getStyle().set("padding", "0.25rem 0.75rem");
            statusBadge.getStyle().set("border-radius", "12px");
            statusBadge.getStyle().set("font-size", "0.875rem");
            statusBadge.getStyle().set("font-weight", "500");
            if (activeDeliveries == 0) {
                statusBadge.getStyle().set("color", "var(--lumo-success-text-color)");
                statusBadge.getStyle().set("background-color", "var(--lumo-success-color-10pct)");
            } else {
                statusBadge.getStyle().set("color", "var(--lumo-warning-text-color)");
                statusBadge.getStyle().set("background-color", "var(--lumo-warning-color-10pct)");
            }
            return statusBadge;
        })).setHeader("Status").setAutoWidth(true);
        
        couriersGrid.addColumn(new ComponentRenderer<>(courier -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(true);
            
            Button editBtn = new Button("Edytuj", e -> showEditCourierDialog(courier));
            Button deleteBtn = new Button("Usuń", e -> confirmDeleteCourier(courier));
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            
            actions.add(editBtn, deleteBtn);
            return actions;
        })).setHeader("Akcje").setAutoWidth(true);
        
        // Apply filter
        filterCombo.addValueChangeListener(e -> {
            String filter = e.getValue();
            List<CourierDto> filtered = couriers;
            if (filter != null) {
                switch (filter) {
                    case "Dostępni":
                        // Kurierzy bez aktywnych dostaw
                        filtered = couriers.stream()
                            .filter(c -> {
                                long activeDeliveries = orders.stream()
                                    .filter(o -> o.courierId() != null && o.courierId().equals(c.id()) && "IN_DELIVERY".equals(o.status()))
                                    .count();
                                return activeDeliveries == 0;
                            })
                            .collect(Collectors.toList());
                        break;
                    case "W trakcie dostawy":
                        // Kurierzy z aktywnymi dostawami
                        filtered = couriers.stream()
                            .filter(c -> {
                                long activeDeliveries = orders.stream()
                                    .filter(o -> o.courierId() != null && o.courierId().equals(c.id()) && "IN_DELIVERY".equals(o.status()))
                                    .count();
                                return activeDeliveries > 0;
                            })
                            .collect(Collectors.toList());
                        break;
                }
            }
            couriersGrid.setItems(filtered);
        });
        
        couriersGrid.setItems(couriers);
        couriersGrid.setSizeFull();
        
        layout.add(header, couriersGrid);
        layout.setFlexGrow(1, couriersGrid);
        
        contentContainer.add(layout);
    }
    
    private void showOrderDetails(OrderDto order) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Szczegóły zamówienia #" + order.id());
        dialog.setModal(true);
        
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        
        content.add(new Span("Status: " + order.status()));
        content.add(new Span("Wartość: " + String.format("%.2f zł", order.totalPrice())));
        content.add(new Span("Klient: " + order.userEmail()));
        if (order.courierFullName() != null) {
            content.add(new Span("Kurier: " + order.courierFullName()));
        } else if (order.courierEmail() != null) {
            content.add(new Span("Kurier: " + order.courierEmail()));
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        content.add(new Span("Data: " + (order.createdAt() != null ? order.createdAt().format(formatter) : "")));
        
        if (order.items() != null && !order.items().isEmpty()) {
            H3 itemsTitle = new H3("Produkty:");
            content.add(itemsTitle);
            order.items().forEach(item -> {
                content.add(new Span(String.format("%s x%d - %.2f zł", 
                    item.dishName(), item.quantity(), item.priceSnapshot() * item.quantity())));
            });
        }
        
        Button closeBtn = new Button("Zamknij", e -> dialog.close());
        dialog.getFooter().add(closeBtn);
        
        dialog.add(content);
        dialog.open();
    }
    
    private void showAssignCourierDialog(OrderDto order) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Przypisz kuriera do zamówienia #" + order.id());
        dialog.setModal(true);
        
        // Pokaż wszystkich kurierów - kurier może mieć wiele zamówień jednocześnie
        if (couriers.isEmpty()) {
            VerticalLayout content = new VerticalLayout();
            content.setSpacing(true);
            content.add(new Span("Brak kurierów przypisanych do restauracji."));
            Button closeBtn = new Button("Zamknij", e -> dialog.close());
            dialog.add(content);
            dialog.getFooter().add(closeBtn);
            dialog.open();
            return;
        }
        
        ComboBox<CourierDto> courierCombo = new ComboBox<>("Wybierz kuriera");
        courierCombo.setItems(couriers);
        courierCombo.setItemLabelGenerator(c -> {
            // Policz aktywne dostawy dla kuriera
            long activeDeliveries = orders.stream()
                .filter(o -> o.courierId() != null && o.courierId().equals(c.id()) && "IN_DELIVERY".equals(o.status()))
                .count();
            if (activeDeliveries > 0) {
                return c.fullName() + " (" + c.email() + ") - " + activeDeliveries + " aktywnych dostaw";
            } else {
                return c.fullName() + " (" + c.email() + ") - Dostępny";
            }
        });
        courierCombo.setWidthFull();
        
        Button assignBtn = new Button("Przypisz", e -> {
            CourierDto selected = courierCombo.getValue();
            if (selected != null) {
                assignCourier(order.id(), selected.id());
                dialog.close();
            } else {
                Notification.show("Wybierz kuriera", 3000, Notification.Position.TOP_CENTER);
            }
        });
        assignBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        Button cancelBtn = new Button("Anuluj", e -> dialog.close());
        
        VerticalLayout content = new VerticalLayout(courierCombo);
        content.setSpacing(true);
        dialog.add(content);
        dialog.getFooter().add(cancelBtn, assignBtn);
        dialog.open();
    }
    
    private void showDishDialog(DishDto existingDish) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existingDish == null ? "Dodaj danie" : "Edytuj danie");
        dialog.setModal(true);
        dialog.setWidth("500px");
        
        TextField nameField = new TextField("Nazwa");
        nameField.setWidthFull();
        if (existingDish != null) {
            nameField.setValue(existingDish.name());
        }
        
        TextArea descriptionField = new TextArea("Opis");
        descriptionField.setWidthFull();
        descriptionField.setMinHeight("100px");
        if (existingDish != null) {
            descriptionField.setValue(existingDish.description() != null ? existingDish.description() : "");
        }
        
        NumberField priceField = new NumberField("Cena (zł)");
        priceField.setWidthFull();
        if (existingDish != null) {
            priceField.setValue(existingDish.price());
        }
        
        ComboBox<String> categoryField = new ComboBox<>("Kategoria");
        categoryField.setItems("PIZZA", "BURGER", "SUSHI", "KEBAB", "MEXICAN", "ASIAN", "ITALIAN");
        categoryField.setWidthFull();
        if (existingDish != null && existingDish.category() != null) {
            categoryField.setValue(existingDish.category());
        }
        
        TextField imageUrlField = new TextField("Link do zdjęcia (URL)");
        imageUrlField.setWidthFull();
        imageUrlField.setPlaceholder("https://example.com/image.jpg");
        if (existingDish != null && existingDish.imageUrl() != null) {
            imageUrlField.setValue(existingDish.imageUrl());
        }
        
        com.vaadin.flow.component.checkbox.Checkbox availableCheckbox = new com.vaadin.flow.component.checkbox.Checkbox("Dostępne");
        if (existingDish != null) {
            availableCheckbox.setValue(existingDish.available());
        } else {
            availableCheckbox.setValue(true);
        }
        
        Button saveBtn = new Button("Zapisz", e -> {
            String name = nameField.getValue();
            String desc = descriptionField.getValue();
            Double price = priceField.getValue();
            String cat = categoryField.getValue();
            String imgUrl = imageUrlField.getValue();
            Boolean avail = availableCheckbox.getValue();
            
            if (name == null || name.isEmpty() || price == null) {
                 Notification.show("Wypełnij wymagane pola (Nazwa, Cena)", 3000, Notification.Position.TOP_CENTER);
                 return;
            }
            
            if (existingDish == null) {
                createDish(name, desc, price, cat, imgUrl);
            } else {
                updateDish(existingDish.id(), name, desc, price, cat, avail, imgUrl);
            }
            dialog.close();
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        Button cancelBtn = new Button("Anuluj", e -> dialog.close());
        
        VerticalLayout content = new VerticalLayout(nameField, descriptionField, priceField, categoryField, imageUrlField, availableCheckbox);
        content.setSpacing(true);
        dialog.add(content);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }
    
    private void showCourierDialog() {
        showCourierDialog(null);
    }
    
    private void showCourierDialog(CourierDto existingCourier) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existingCourier == null ? "Dodaj kuriera" : "Edytuj kuriera");
        dialog.setModal(true);
        
        com.vaadin.flow.component.textfield.EmailField emailField = new com.vaadin.flow.component.textfield.EmailField("Email");
        emailField.setWidthFull();
        emailField.setRequired(true);
        emailField.setErrorMessage("Podaj prawidłowy adres email");
        if (existingCourier != null) {
            emailField.setValue(existingCourier.email());
        }
        
        TextField fullNameField = new TextField("Imię i nazwisko");
        fullNameField.setWidthFull();
        if (existingCourier != null) {
            fullNameField.setValue(existingCourier.fullName());
        }
        
        com.vaadin.flow.component.textfield.PasswordField passwordField = new com.vaadin.flow.component.textfield.PasswordField("Hasło");
        passwordField.setWidthFull();
        passwordField.setVisible(existingCourier == null);
        passwordField.setRequired(existingCourier == null);
        
        Button saveBtn = new Button("Zapisz", e -> {
            if (existingCourier == null) {
                createCourier(emailField.getValue(), fullNameField.getValue(), passwordField.getValue());
            } else {
                updateCourier(existingCourier.id(), emailField.getValue(), fullNameField.getValue());
            }
            dialog.close();
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        Button cancelBtn = new Button("Anuluj", e -> dialog.close());
        
        VerticalLayout content = new VerticalLayout(emailField, fullNameField, passwordField);
        content.setSpacing(true);
        dialog.add(content);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }
    
    private void showEditCourierDialog(CourierDto courier) {
        showCourierDialog(courier);
    }
    
    private void printReceipt(OrderDto order) {
        try {
            if (restaurant == null) {
                Notification.show("Błąd: Brak danych restauracji", 3000, Notification.Position.TOP_CENTER);
                return;
            }
            
            byte[] pdfBytes = pdfService.generateReceipt(order, restaurant);
            StreamResource resource = new StreamResource(
                "bon_zamowienia_" + order.id() + ".pdf",
                () -> new ByteArrayInputStream(pdfBytes)
            );
            resource.setContentType("application/pdf");
            
            getUI().ifPresent(ui -> {
                // Register the resource and get its URL
                com.vaadin.flow.server.StreamRegistration registration = 
                    ui.getSession().getResourceRegistry().registerResource(resource);
                String url = registration.getResourceUri().toString();
                ui.getPage().open(url, "_blank");
            });
            
            Notification.show("Bon został wygenerowany", 2000, Notification.Position.TOP_CENTER);
        } catch (Exception e) {
            Notification.show("Błąd podczas generowania bonu: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER);
        }
    }
    
    // Service calls
    private void updateOrderStatus(Long orderId, String status) {
        try {
            String email = getAdminEmail();
            if (email == null) {
                Notification.show("Błąd autoryzacji", 3000, Notification.Position.TOP_CENTER);
                return;
            }
            restaurantService.updateOrderStatus(email, orderId, status);
            Notification.show("Status zamówienia zaktualizowany", 3000, Notification.Position.TOP_CENTER);
            loadRestaurantData();
            showOrdersTab();
        } catch (Exception e) {
            Notification.show("Błąd: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER);
        }
    }
    
    private void assignCourier(Long orderId, Long courierId) {
        try {
            String email = getAdminEmail();
            if (email == null) {
                Notification.show("Błąd autoryzacji", 3000, Notification.Position.TOP_CENTER);
                return;
            }
            restaurantService.assignCourier(email, orderId, courierId);
            Notification.show("Kurier przypisany", 3000, Notification.Position.TOP_CENTER);
            loadRestaurantData();
            showOrdersTab();
        } catch (Exception e) {
            Notification.show("Błąd: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER);
        }
    }
    
    private void createDish(String name, String description, Double price, String category, String imageUrl) {
        try {
            String email = getAdminEmail();
            if (email == null) {
                Notification.show("Błąd autoryzacji", 3000, Notification.Position.TOP_CENTER);
                return;
            }
            DishCreateDto dto = new DishCreateDto(name, description, price, category, imageUrl);
            restaurantService.addDish(email, dto);
            Notification.show("Danie dodane", 3000, Notification.Position.TOP_CENTER);
            loadRestaurantData();
            showDishesTab();
        } catch (Exception e) {
            Notification.show("Błąd: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER);
        }
    }
    
    private void updateDish(Long dishId, String name, String description, Double price, String category, Boolean available, String imageUrl) {
        try {
            String email = getAdminEmail();
            if (email == null) {
                Notification.show("Błąd autoryzacji", 3000, Notification.Position.TOP_CENTER);
                return;
            }
            DishUpdateDto dto = new DishUpdateDto(name, description, price, available, category, imageUrl);
            restaurantService.updateDish(email, dishId, dto);
            Notification.show("Danie zaktualizowane", 3000, Notification.Position.TOP_CENTER);
            loadRestaurantData();
            showDishesTab();
        } catch (Exception e) {
            Notification.show("Błąd: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER);
        }
    }
    
    private void confirmDeleteDish(DishDto dish) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Potwierdzenie usunięcia");
        dialog.setText("Czy na pewno chcesz usunąć danie \"" + dish.name() + "\"?");
        dialog.setCancelable(true);
        dialog.setConfirmText("Usuń");
        dialog.setConfirmButtonTheme("error primary");
        dialog.setCancelText("Anuluj");
        dialog.addConfirmListener(e -> deleteDish(dish.id()));
        dialog.open();
    }
    
    private void deleteDish(Long dishId) {
        try {
            String email = getAdminEmail();
            if (email == null) {
                Notification.show("Błąd autoryzacji", 3000, Notification.Position.TOP_CENTER);
                return;
            }
            restaurantService.deleteDish(email, dishId);
            Notification.show("Danie usunięte", 3000, Notification.Position.TOP_CENTER);
            loadRestaurantData();
            showDishesTab();
        } catch (Exception e) {
            Notification.show("Błąd: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER);
        }
    }
    
    private void createCourier(String email, String fullName, String password) {
        try {
            String adminEmail = getAdminEmail();
            if (adminEmail == null) {
                Notification.show("Błąd autoryzacji", 3000, Notification.Position.TOP_CENTER);
                return;
            }
            CourierCreateDto dto = new CourierCreateDto(email, fullName, password);
            restaurantService.createCourier(adminEmail, dto);
            Notification.show("Kurier dodany", 3000, Notification.Position.TOP_CENTER);
            loadRestaurantData();
            showCouriersTab();
        } catch (Exception e) {
            Notification.show("Błąd: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER);
        }
    }
    
    private void updateCourier(Long courierId, String email, String fullName) {
        try {
            String adminEmail = getAdminEmail();
            if (adminEmail == null) {
                Notification.show("Błąd autoryzacji", 3000, Notification.Position.TOP_CENTER);
                return;
            }
            CourierUpdateDto dto = new CourierUpdateDto(email, fullName);
            restaurantService.updateCourier(adminEmail, courierId, dto);
            Notification.show("Kurier zaktualizowany", 3000, Notification.Position.TOP_CENTER);
            loadRestaurantData();
            showCouriersTab();
        } catch (Exception e) {
            Notification.show("Błąd: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER);
        }
    }
    
    private void confirmDeleteCourier(CourierDto courier) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Potwierdzenie usunięcia");
        dialog.setText("Czy na pewno chcesz usunąć kuriera \"" + courier.fullName() + "\"?");
        dialog.setCancelable(true);
        dialog.setConfirmText("Usuń");
        dialog.setConfirmButtonTheme("error primary");
        dialog.setCancelText("Anuluj");
        dialog.addConfirmListener(e -> deleteCourier(courier.id()));
        dialog.open();
    }
    
    private void deleteCourier(Long courierId) {
        try {
            String email = getAdminEmail();
            if (email == null) {
                Notification.show("Błąd autoryzacji", 3000, Notification.Position.TOP_CENTER);
                return;
            }
            restaurantService.deleteCourier(email, courierId);
            Notification.show("Kurier usunięty", 3000, Notification.Position.TOP_CENTER);
            loadRestaurantData();
            showCouriersTab();
        } catch (Exception e) {
            Notification.show("Błąd: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER);
        }
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

    
    private void exportOrdersToExcel() {
        try {
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Zamówienia");
            
            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            
            // Create data style
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            
            // Create date style
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.cloneStyleFrom(dataStyle);
            CreationHelper createHelper = workbook.getCreationHelper();
            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd.mm.yyyy hh:mm"));
            
            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "Status", "Wartość", "Klient", "Kurier", "Data"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Create data rows
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
            int rowNum = 1;
            for (OrderDto order : orders) {
                Row row = sheet.createRow(rowNum++);
                
                // ID
                Cell idCell = row.createCell(0);
                idCell.setCellValue(order.id());
                idCell.setCellStyle(dataStyle);
                
                // Status
                Cell statusCell = row.createCell(1);
                statusCell.setCellValue(order.status() != null ? getStatusLabel(order.status()) : "");
                statusCell.setCellStyle(dataStyle);
                
                // Wartość
                Cell priceCell = row.createCell(2);
                priceCell.setCellValue(order.totalPrice());
                priceCell.setCellStyle(dataStyle);
                
                // Klient
                Cell clientCell = row.createCell(3);
                clientCell.setCellValue(order.userEmail() != null ? order.userEmail() : "");
                clientCell.setCellStyle(dataStyle);
                
                // Kurier
                Cell courierCell = row.createCell(4);
                String courierName = order.courierFullName() != null ? order.courierFullName() : 
                    (order.courierEmail() != null ? order.courierEmail() : "");
                courierCell.setCellValue(courierName);
                courierCell.setCellStyle(dataStyle);
                
                // Data
                Cell dateCell = row.createCell(5);
                if (order.createdAt() != null) {
                    dateCell.setCellValue(order.createdAt().format(formatter));
                } else {
                    dateCell.setCellValue("");
                }
                dateCell.setCellStyle(dataStyle);
            }
            
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // Write to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            workbook.close();
            
            StreamResource resource = new StreamResource(
                "zamowienia_" + java.time.LocalDate.now() + ".xlsx",
                () -> new ByteArrayInputStream(baos.toByteArray())
            );
            resource.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            
            getUI().ifPresent(ui -> {
                com.vaadin.flow.server.StreamRegistration registration = 
                    ui.getSession().getResourceRegistry().registerResource(resource);
                String url = registration.getResourceUri().toString();
                ui.getPage().open(url, "_blank");
            });
            
            Notification.show("Eksport zakończony pomyślnie", 2000, Notification.Position.TOP_CENTER);
        } catch (Exception e) {
            Notification.show("Błąd podczas eksportu: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER);
        }
    }
    
    private void showStatisticsTab() {
        contentContainer.removeAll();
        
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(false);
        layout.setPadding(true);
        layout.setSizeFull();
        
        H2 title = new H2("Statystyki i Analiza");
        title.getStyle().set("margin-top", "0");
        title.getStyle().set("margin-bottom", "0.5rem");
        Button refreshBtn = new Button("Odśwież", VaadinIcon.REFRESH.create(), e -> {
            loadRestaurantData();
            showStatisticsTab();
        });
        
        HorizontalLayout header = new HorizontalLayout(title, refreshBtn);
        header.setWidthFull();
        header.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        header.getStyle().set("margin-bottom", "1rem");
        
        try {
            String email = getAdminEmail();
            if (email == null) {
                Notification.show("Błąd autoryzacji", 3000, Notification.Position.TOP_CENTER);
                return;
            }
            
            // Get statistics data
            double todayRevenue = restaurantService.getTodayRevenue(email);
            long todayOrders = restaurantService.getTodayOrdersCount(email);
            Map<String, Integer> topDishes = restaurantService.getTopDishes(email, 10);
            
            // Calculate weekly and monthly stats
            LocalDateTime weekStart = LocalDateTime.now().minusDays(7).withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime monthStart = LocalDateTime.now().minusDays(30).withHour(0).withMinute(0).withSecond(0).withNano(0);
            
            List<OrderDto> allOrders = restaurantService.listOrders(email);
            double weekRevenue = allOrders.stream()
                .filter(o -> o.createdAt() != null && o.createdAt().isAfter(weekStart) && "DELIVERED".equals(o.status()))
                .mapToDouble(OrderDto::totalPrice)
                .sum();
            
            double monthRevenue = allOrders.stream()
                .filter(o -> o.createdAt() != null && o.createdAt().isAfter(monthStart) && "DELIVERED".equals(o.status()))
                .mapToDouble(OrderDto::totalPrice)
                .sum();
            
            long weekOrders = allOrders.stream()
                .filter(o -> o.createdAt() != null && o.createdAt().isAfter(weekStart))
                .count();
            
            long monthOrders = allOrders.stream()
                .filter(o -> o.createdAt() != null && o.createdAt().isAfter(monthStart))
                .count();
            
            // Calculate average order value
            double avgOrderValue = allOrders.stream()
                .filter(o -> "DELIVERED".equals(o.status()))
                .mapToDouble(OrderDto::totalPrice)
                .average()
                .orElse(0.0);
            
            // Calculate courier statistics
            Map<String, Long> courierStats = allOrders.stream()
                .filter(o -> o.courierFullName() != null)
                .collect(Collectors.groupingBy(
                    o -> o.courierFullName(),
                    Collectors.counting()
                ));
            
            // Main stats in 2 columns layout
            HorizontalLayout mainStatsLayout = new HorizontalLayout();
            mainStatsLayout.setWidthFull();
            mainStatsLayout.setSpacing(true);
            mainStatsLayout.getStyle().set("margin-bottom", "1rem");
            
            // Left column: Revenue and Orders
            VerticalLayout leftColumn = new VerticalLayout();
            leftColumn.setSpacing(false);
            leftColumn.setPadding(false);
            leftColumn.getStyle().set("flex", "1");
            leftColumn.getStyle().set("min-width", "0");
            
            H3 revenueSectionTitle = new H3("💰 Przychody");
            revenueSectionTitle.getStyle().set("margin-top", "0");
            revenueSectionTitle.getStyle().set("margin-bottom", "0.5rem");
            revenueSectionTitle.getStyle().set("color", "var(--text-primary)");
            revenueSectionTitle.getStyle().set("font-size", "1rem");
            
            Div revenueCards = new Div();
            revenueCards.getStyle().set("display", "grid");
            revenueCards.getStyle().set("grid-template-columns", "repeat(4, 1fr)");
            revenueCards.getStyle().set("gap", "0.75rem");
            revenueCards.getStyle().set("margin-bottom", "1rem");
            
            revenueCards.add(createStatCard("Dzisiaj", String.format("%.2f zł", todayRevenue), VaadinIcon.MONEY));
            revenueCards.add(createStatCard("Tydzień", String.format("%.2f zł", weekRevenue), VaadinIcon.CALENDAR));
            revenueCards.add(createStatCard("Miesiąc", String.format("%.2f zł", monthRevenue), VaadinIcon.CALENDAR_CLOCK));
            revenueCards.add(createStatCard("Średnia", String.format("%.2f zł", avgOrderValue), VaadinIcon.EURO));
            
            H3 ordersSectionTitle = new H3("📋 Zamówienia");
            ordersSectionTitle.getStyle().set("margin-top", "0");
            ordersSectionTitle.getStyle().set("margin-bottom", "0.5rem");
            ordersSectionTitle.getStyle().set("color", "var(--text-primary)");
            ordersSectionTitle.getStyle().set("font-size", "1rem");
            
            Div ordersCards = new Div();
            ordersCards.getStyle().set("display", "grid");
            ordersCards.getStyle().set("grid-template-columns", "repeat(3, 1fr)");
            ordersCards.getStyle().set("gap", "0.75rem");
            
            ordersCards.add(createStatCard("Dzisiaj", String.valueOf(todayOrders), VaadinIcon.CLIPBOARD_TEXT));
            ordersCards.add(createStatCard("Tydzień", String.valueOf(weekOrders), VaadinIcon.CALENDAR));
            ordersCards.add(createStatCard("Miesiąc", String.valueOf(monthOrders), VaadinIcon.CALENDAR_CLOCK));
            
            leftColumn.add(revenueSectionTitle, revenueCards, ordersSectionTitle, ordersCards);
            
            // Right column: Analysis
            VerticalLayout rightColumn = new VerticalLayout();
            rightColumn.setSpacing(false);
            rightColumn.setPadding(false);
            rightColumn.getStyle().set("flex", "1");
            rightColumn.getStyle().set("min-width", "0");
            
            HorizontalLayout analysisLayout = new HorizontalLayout();
            analysisLayout.setWidthFull();
            analysisLayout.setSpacing(true);
            analysisLayout.getStyle().set("margin-bottom", "0");
            
            // Top dishes section
            Div topDishesDiv = new Div();
            topDishesDiv.getStyle().set("background", "var(--bg-primary)");
            topDishesDiv.getStyle().set("border", "1px solid var(--border-color)");
            topDishesDiv.getStyle().set("border-radius", "8px");
            topDishesDiv.getStyle().set("padding", "1rem");
            topDishesDiv.getStyle().set("box-shadow", "var(--shadow-sm)");
            topDishesDiv.getStyle().set("flex", "1");
            topDishesDiv.getStyle().set("min-width", "0");
            
            H3 topDishesTitle = new H3("🍽️ Najpopularniejsze dania");
            topDishesTitle.getStyle().set("margin-top", "0");
            topDishesTitle.getStyle().set("margin-bottom", "0.5rem");
            topDishesTitle.getStyle().set("color", "var(--text-primary)");
            topDishesTitle.getStyle().set("font-size", "0.95rem");
            topDishesDiv.add(topDishesTitle);
            
            if (topDishes.isEmpty()) {
                Span noData = new Span("Brak danych");
                noData.getStyle().set("color", "var(--text-secondary)");
                topDishesDiv.add(noData);
            } else {
                VerticalLayout dishesList = new VerticalLayout();
                dishesList.setSpacing(false);
                dishesList.setPadding(false);
                
                int rank = 1;
                int maxItems = Math.min(5, topDishes.size());
                for (Map.Entry<String, Integer> entry : topDishes.entrySet()) {
                    if (rank > maxItems) break;
                    HorizontalLayout dishRow = new HorizontalLayout();
                    dishRow.setWidthFull();
                    dishRow.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);
                    dishRow.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
                    dishRow.getStyle().set("padding", "0.25rem 0");
                    
                    Span rankSpan = new Span(rank + ". " + entry.getKey());
                    rankSpan.getStyle().set("font-weight", "500");
                    rankSpan.getStyle().set("color", "var(--text-primary)");
                    rankSpan.getStyle().set("font-size", "0.875rem");
                    
                    Span countSpan = new Span(entry.getValue() + " szt.");
                    countSpan.getStyle().set("color", "var(--text-secondary)");
                    countSpan.getStyle().set("font-size", "0.875rem");
                    
                    dishRow.add(rankSpan, countSpan);
                    dishesList.add(dishRow);
                    rank++;
                }
                topDishesDiv.add(dishesList);
            }
            
            // Courier statistics section
            Div courierStatsDiv = new Div();
            courierStatsDiv.getStyle().set("background", "var(--bg-primary)");
            courierStatsDiv.getStyle().set("border", "1px solid var(--border-color)");
            courierStatsDiv.getStyle().set("border-radius", "8px");
            courierStatsDiv.getStyle().set("padding", "1rem");
            courierStatsDiv.getStyle().set("box-shadow", "var(--shadow-sm)");
            courierStatsDiv.getStyle().set("flex", "1");
            courierStatsDiv.getStyle().set("min-width", "0");
            
            H3 courierStatsTitle = new H3("🚚 Statystyki kurierów");
            courierStatsTitle.getStyle().set("margin-top", "0");
            courierStatsTitle.getStyle().set("margin-bottom", "0.5rem");
            courierStatsTitle.getStyle().set("color", "var(--text-primary)");
            courierStatsTitle.getStyle().set("font-size", "0.95rem");
            courierStatsDiv.add(courierStatsTitle);
            
            if (courierStats.isEmpty()) {
                Span noData = new Span("Brak danych o kurierach");
                noData.getStyle().set("color", "var(--text-secondary)");
                courierStatsDiv.add(noData);
            } else {
                VerticalLayout courierList = new VerticalLayout();
                courierList.setSpacing(false);
                courierList.setPadding(false);
                
                courierStats.entrySet().stream()
                    .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                    .forEach(entry -> {
                        HorizontalLayout courierRow = new HorizontalLayout();
                        courierRow.setWidthFull();
                        courierRow.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);
                        courierRow.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
                        courierRow.getStyle().set("padding", "0.25rem 0");
                        
                        Span nameSpan = new Span(entry.getKey());
                        nameSpan.getStyle().set("font-weight", "500");
                        nameSpan.getStyle().set("color", "var(--text-primary)");
                        nameSpan.getStyle().set("font-size", "0.875rem");
                        
                        Span countSpan = new Span(entry.getValue() + " dostaw");
                        countSpan.getStyle().set("color", "var(--text-secondary)");
                        countSpan.getStyle().set("font-size", "0.875rem");
                        
                        courierRow.add(nameSpan, countSpan);
                        courierList.add(courierRow);
                    });
                courierStatsDiv.add(courierList);
            }
            
            analysisLayout.add(topDishesDiv, courierStatsDiv);
            rightColumn.add(analysisLayout);
            
            // Charts section - using simple HTML canvas with Chart.js
            H3 chartsSectionTitle = new H3("📈 Wykres");
            chartsSectionTitle.getStyle().set("margin-top", "0");
            chartsSectionTitle.getStyle().set("margin-bottom", "0.5rem");
            chartsSectionTitle.getStyle().set("color", "var(--text-primary)");
            chartsSectionTitle.getStyle().set("font-size", "1rem");
            
            Div chartsDiv = new Div();
            chartsDiv.getStyle().set("background", "var(--bg-primary)");
            chartsDiv.getStyle().set("border", "1px solid var(--border-color)");
            chartsDiv.getStyle().set("border-radius", "8px");
            chartsDiv.getStyle().set("padding", "1rem");
            chartsDiv.getStyle().set("box-shadow", "var(--shadow-sm)");
            
            chartsDiv.add(chartsSectionTitle);
            
            // Prepare data for charts as JSON
            String labelsJson = topDishes.entrySet().stream()
                .limit(5)
                .map(e -> "\"" + e.getKey().replace("\"", "\\\"") + "\"")
                .collect(Collectors.joining(","));
            String valuesJson = topDishes.entrySet().stream()
                .limit(5)
                .map(e -> String.valueOf(e.getValue()))
                .collect(Collectors.joining(","));
            
            // Create chart containers
            Div chartContainer = new Div();
            chartContainer.getStyle().set("position", "relative");
            chartContainer.getStyle().set("height", "250px");
            chartContainer.getStyle().set("margin-top", "0.5rem");
            String containerId = "chart-container-" + System.currentTimeMillis();
            chartContainer.setId(containerId);
            
            chartsDiv.add(chartContainer);
            rightColumn.add(chartsDiv);
            
            mainStatsLayout.add(leftColumn, rightColumn);
            
            // Add Chart.js and create charts
            getElement().executeJs(
                "if (typeof Chart === 'undefined') {" +
                "  const script = document.createElement('script');" +
                "  script.src = 'https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js';" +
                "  script.onload = function() { setTimeout(function() { createChart($0, $1, $2); }, 100); };" +
                "  document.head.appendChild(script);" +
                "} else { createChart($0, $1, $2); }" +
                "function createChart(containerId, labelsJson, valuesJson) {" +
                "  const container = document.getElementById(containerId);" +
                "  if (!container) { console.error('Container not found:', containerId); return; }" +
                "  container.innerHTML = '';" +
                "  " +
                "  const labels = JSON.parse('[' + labelsJson + ']');" +
                "  const values = JSON.parse('[' + valuesJson + ']');" +
                "  " +
                "  const dishesCanvas = document.createElement('canvas');" +
                "  dishesCanvas.id = 'dishes-chart-' + containerId;" +
                "  dishesCanvas.style.maxHeight = '300px';" +
                "  container.appendChild(dishesCanvas);" +
                "  " +
                "  const dishesCtx = dishesCanvas.getContext('2d');" +
                "  const isDark = document.documentElement.getAttribute('data-theme') === 'dark';" +
                "  const textColor = isDark ? '#ffffff' : '#1a1a1a';" +
                "  " +
                "  new Chart(dishesCtx, {" +
                "    type: 'bar'," +
                "    data: {" +
                "      labels: labels," +
                "      datasets: [{" +
                "        label: 'Sprzedane sztuki'," +
                "        data: values," +
                "        backgroundColor: isDark ? 'rgba(77, 158, 255, 0.6)' : 'rgba(0, 102, 255, 0.6)'," +
                "        borderColor: isDark ? 'rgba(77, 158, 255, 1)' : 'rgba(0, 102, 255, 1)'," +
                "        borderWidth: 1" +
                "      }]" +
                "    }," +
                "    options: {" +
                "      responsive: true," +
                "      maintainAspectRatio: false," +
                "      plugins: {" +
                "        legend: { display: false, labels: { color: textColor } }," +
                "        title: { display: true, text: 'Top 5 dań', color: textColor }" +
                "      }," +
                "      scales: {" +
                "        y: { beginAtZero: true, ticks: { color: textColor }, grid: { color: isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.1)' } }," +
                "        x: { ticks: { color: textColor }, grid: { color: isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.1)' } }" +
                "      }" +
                "    }" +
                "  });" +
                "}",
                containerId,
                labelsJson,
                valuesJson
            );
            
            layout.add(header, mainStatsLayout);
            
        } catch (Exception e) {
            Notification.show("Błąd podczas ładowania statystyk: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER);
        }
        
        contentContainer.add(layout);
    }
}
