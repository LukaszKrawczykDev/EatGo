package pollub.eatgo.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
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
import pollub.eatgo.dto.dish.DishCreateDto;
import pollub.eatgo.dto.dish.DishDto;
import pollub.eatgo.dto.dish.DishUpdateDto;
import pollub.eatgo.dto.order.OrderDto;
import pollub.eatgo.dto.restaurant.RestaurantDto;
import pollub.eatgo.dto.restaurant.RestaurantUpdateDto;
import pollub.eatgo.service.AuthenticationService;
import pollub.eatgo.service.RestaurantService;
import pollub.eatgo.service.TokenValidationService;
import pollub.eatgo.views.components.HeaderComponent;

import java.util.ArrayList;
import java.util.List;

@Route("restaurant")
@PageTitle("EatGo - Panel Restauracji")
public class RestaurantAdminView extends VerticalLayout implements BeforeEnterObserver {
    
    private final AuthenticationService authService;
    private final RestaurantService restaurantService;
    private final TokenValidationService tokenValidationService;
    
    private Tabs tabs;
    private Div contentContainer;
    
    // Tabs
    private Tab ordersTab;
    private Tab dishesTab;
    private Tab couriersTab;
    private Tab settingsTab;
    
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
                               TokenValidationService tokenValidationService) {
        this.authService = authService;
        this.restaurantService = restaurantService;
        this.tokenValidationService = tokenValidationService;
        
        setSizeFull();
        setSpacing(false);
        setPadding(false);
        addClassName("restaurant-admin-view");
        
        HeaderComponent header = new HeaderComponent(authService, tokenValidationService);
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
        
        settingsTab = new Tab();
        settingsTab.add(VaadinIcon.COG.create(), new Span("Ustawienia"));
        
        tabs.add(ordersTab, dishesTab, couriersTab, settingsTab);
        
        tabs.addSelectedChangeListener(e -> {
            Tab selected = e.getSelectedTab();
            if (selected == ordersTab) {
                showOrdersTab();
            } else if (selected == dishesTab) {
                showDishesTab();
            } else if (selected == couriersTab) {
                showCouriersTab();
            } else if (selected == settingsTab) {
                showSettingsTab();
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
        
        H2 title = new H2("Zamówienia");
        Button refreshBtn = new Button("Odśwież", VaadinIcon.REFRESH.create(), e -> {
            loadRestaurantData();
            showOrdersTab();
        });
        
        HorizontalLayout header = new HorizontalLayout(title, refreshBtn);
        header.setWidthFull();
        header.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        
        ordersGrid = new Grid<>(OrderDto.class, false);
        ordersGrid.addColumn(OrderDto::id).setHeader("ID").setAutoWidth(true);
        ordersGrid.addColumn(OrderDto::status).setHeader("Status").setAutoWidth(true);
        ordersGrid.addColumn(o -> String.format("%.2f zł", o.totalPrice())).setHeader("Wartość").setAutoWidth(true);
        ordersGrid.addColumn(OrderDto::userEmail).setHeader("Klient").setAutoWidth(true);
        ordersGrid.addColumn(OrderDto::courierEmail).setHeader("Kurier").setAutoWidth(true);
        ordersGrid.addColumn(OrderDto::createdAt).setHeader("Data").setAutoWidth(true);
        
        ordersGrid.addColumn(new ComponentRenderer<>(order -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(true);
            
            if ("PLACED".equals(order.status())) {
                Button acceptBtn = new Button("Przyjmij", e -> updateOrderStatus(order.id(), "ACCEPTED"));
                acceptBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
                actions.add(acceptBtn);
            } else if ("ACCEPTED".equals(order.status())) {
                Button prepareBtn = new Button("Przygotuj", e -> updateOrderStatus(order.id(), "PREPARING"));
                prepareBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                actions.add(prepareBtn);
            } else if ("PREPARING".equals(order.status())) {
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
            actions.add(detailsBtn);
            
            return actions;
        })).setHeader("Akcje").setAutoWidth(true);
        
        ordersGrid.setItems(orders);
        ordersGrid.setSizeFull();
        
        layout.add(header, ordersGrid);
        layout.setFlexGrow(1, ordersGrid);
        
        contentContainer.add(layout);
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
        
        HorizontalLayout header = new HorizontalLayout(title, addBtn, refreshBtn);
        header.setWidthFull();
        header.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        
        dishesGrid = new Grid<>(DishDto.class, false);
        dishesGrid.addColumn(DishDto::name).setHeader("Nazwa").setAutoWidth(true);
        dishesGrid.addColumn(DishDto::description).setHeader("Opis").setAutoWidth(true);
        dishesGrid.addColumn(d -> String.format("%.2f zł", d.price())).setHeader("Cena").setAutoWidth(true);
        dishesGrid.addColumn(d -> d.available() ? "Dostępne" : "Niedostępne").setHeader("Status").setAutoWidth(true);
        dishesGrid.addColumn(DishDto::category).setHeader("Kategoria").setAutoWidth(true);
        
        dishesGrid.addColumn(new ComponentRenderer<>(dish -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(true);
            
            Button editBtn = new Button("Edytuj", e -> showDishDialog(dish));
            Button deleteBtn = new Button("Usuń", e -> deleteDish(dish.id()));
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            
            actions.add(editBtn, deleteBtn);
            return actions;
        })).setHeader("Akcje").setAutoWidth(true);
        
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
        
        HorizontalLayout header = new HorizontalLayout(title, addBtn, refreshBtn);
        header.setWidthFull();
        header.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        
        couriersGrid = new Grid<>(CourierDto.class, false);
        couriersGrid.addColumn(CourierDto::email).setHeader("Email").setAutoWidth(true);
        couriersGrid.addColumn(CourierDto::fullName).setHeader("Imię i nazwisko").setAutoWidth(true);
        
        couriersGrid.addColumn(new ComponentRenderer<>(courier -> {
            Button deleteBtn = new Button("Usuń", e -> deleteCourier(courier.id()));
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            return deleteBtn;
        })).setHeader("Akcje").setAutoWidth(true);
        
        couriersGrid.setItems(couriers);
        couriersGrid.setSizeFull();
        
        layout.add(header, couriersGrid);
        layout.setFlexGrow(1, couriersGrid);
        
        contentContainer.add(layout);
    }
    
    private void showSettingsTab() {
        contentContainer.removeAll();
        
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setPadding(true);
        layout.setMaxWidth("600px");
        
        H2 title = new H2("Ustawienia restauracji");
        
        TextField nameField = new TextField("Nazwa restauracji");
        nameField.setWidthFull();
        if (restaurant != null) {
            nameField.setValue(restaurant.getName());
        }
        
        TextField addressField = new TextField("Adres");
        addressField.setWidthFull();
        if (restaurant != null) {
            addressField.setValue(restaurant.getAddress());
        }
        
        NumberField deliveryPriceField = new NumberField("Cena dostawy (zł)");
        deliveryPriceField.setWidthFull();
        if (restaurant != null) {
            deliveryPriceField.setValue(restaurant.getDeliveryPrice());
        }
        
        Button saveBtn = new Button("Zapisz", e -> {
            if (restaurant != null) {
                updateRestaurant(nameField.getValue(), addressField.getValue(), deliveryPriceField.getValue());
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        layout.add(title, nameField, addressField, deliveryPriceField, saveBtn);
        
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
        if (order.courierEmail() != null) {
            content.add(new Span("Kurier: " + order.courierEmail()));
        }
        content.add(new Span("Data: " + order.createdAt()));
        
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
        
        ComboBox<CourierDto> courierCombo = new ComboBox<>("Wybierz kuriera");
        courierCombo.setItems(couriers);
        courierCombo.setItemLabelGenerator(c -> c.fullName() + " (" + c.email() + ")");
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
        
        TextField nameField = new TextField("Nazwa");
        nameField.setWidthFull();
        if (existingDish != null) {
            nameField.setValue(existingDish.name());
        }
        
        TextArea descriptionField = new TextArea("Opis");
        descriptionField.setWidthFull();
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
        
        com.vaadin.flow.component.checkbox.Checkbox availableCheckbox = new com.vaadin.flow.component.checkbox.Checkbox("Dostępne");
        if (existingDish != null) {
            availableCheckbox.setValue(existingDish.available());
        } else {
            availableCheckbox.setValue(true);
        }
        
        Button saveBtn = new Button("Zapisz", e -> {
            if (existingDish == null) {
                createDish(nameField.getValue(), descriptionField.getValue(), 
                          priceField.getValue(), categoryField.getValue(), availableCheckbox.getValue());
            } else {
                updateDish(existingDish.id(), nameField.getValue(), descriptionField.getValue(), 
                          priceField.getValue(), categoryField.getValue(), availableCheckbox.getValue());
            }
            dialog.close();
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        Button cancelBtn = new Button("Anuluj", e -> dialog.close());
        
        VerticalLayout content = new VerticalLayout(nameField, descriptionField, priceField, categoryField, availableCheckbox);
        content.setSpacing(true);
        dialog.add(content);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }
    
    private void showCourierDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Dodaj kuriera");
        dialog.setModal(true);
        
        TextField emailField = new TextField("Email");
        emailField.setWidthFull();
        
        TextField fullNameField = new TextField("Imię i nazwisko");
        fullNameField.setWidthFull();
        
        com.vaadin.flow.component.textfield.PasswordField passwordField = new com.vaadin.flow.component.textfield.PasswordField("Hasło");
        passwordField.setWidthFull();
        
        Button saveBtn = new Button("Zapisz", e -> {
            createCourier(emailField.getValue(), fullNameField.getValue(), passwordField.getValue());
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
    
    private void createDish(String name, String description, Double price, String category, Boolean available) {
        try {
            String email = getAdminEmail();
            if (email == null) {
                Notification.show("Błąd autoryzacji", 3000, Notification.Position.TOP_CENTER);
                return;
            }
            DishCreateDto dto = new DishCreateDto(name, description, price, category);
            restaurantService.addDish(email, dto);
            Notification.show("Danie dodane", 3000, Notification.Position.TOP_CENTER);
            loadRestaurantData();
            showDishesTab();
        } catch (Exception e) {
            Notification.show("Błąd: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER);
        }
    }
    
    private void updateDish(Long dishId, String name, String description, Double price, String category, Boolean available) {
        try {
            String email = getAdminEmail();
            if (email == null) {
                Notification.show("Błąd autoryzacji", 3000, Notification.Position.TOP_CENTER);
                return;
            }
            DishUpdateDto dto = new DishUpdateDto(name, description, price, available, category);
            restaurantService.updateDish(email, dishId, dto);
            Notification.show("Danie zaktualizowane", 3000, Notification.Position.TOP_CENTER);
            loadRestaurantData();
            showDishesTab();
        } catch (Exception e) {
            Notification.show("Błąd: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER);
        }
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
    
    private void updateRestaurant(String name, String address, Double deliveryPrice) {
        try {
            String email = getAdminEmail();
            if (email == null) {
                Notification.show("Błąd autoryzacji", 3000, Notification.Position.TOP_CENTER);
                return;
            }
            RestaurantUpdateDto dto = new RestaurantUpdateDto(name, address, deliveryPrice);
            restaurant = restaurantService.updateRestaurant(email, dto);
            Notification.show("Ustawienia zaktualizowane", 3000, Notification.Position.TOP_CENTER);
        } catch (Exception e) {
            Notification.show("Błąd: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER);
        }
    }
}
