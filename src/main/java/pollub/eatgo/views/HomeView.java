package pollub.eatgo.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import pollub.eatgo.dto.dish.DishDto;
import pollub.eatgo.dto.restaurant.RestaurantSummaryDto;
import pollub.eatgo.service.AuthenticationService;
import pollub.eatgo.service.RestaurantService;
import pollub.eatgo.views.components.HeaderComponent;
import pollub.eatgo.views.components.FooterComponent;

import java.util.*;
import java.util.stream.Collectors;

@Route("")
@PageTitle("EatGo - Zamów jedzenie online")
public class HomeView extends VerticalLayout {

    private final RestaurantService restaurantService;
    private final AuthenticationService authService;
    private TextField searchField;
    private ComboBox<String> cityComboBox;
    private List<RestaurantSummaryDto> allRestaurants;
    private Div restaurantsContainer;
    private String selectedCity = null;
    private String selectedCategory = null;
    private Div categoriesContainer;
    
    private static final List<String> AVAILABLE_CITIES = Arrays.asList("Warszawa", "Lublin", "Rzeszów");
    private static final Map<String, String> CATEGORY_ICONS = Map.of(
        "PIZZA", "🍕",
        "BURGER", "🍔",
        "SUSHI", "🍣",
        "KEBAB", "🌯",
        "MEXICAN", "🌮",
        "ASIAN", "🍜",
        "ITALIAN", "🍝"
    );

    public HomeView(RestaurantService restaurantService, AuthenticationService authService) {
        this.restaurantService = restaurantService;
        this.authService = authService;
        
        setSizeFull();
        setSpacing(false);
        setPadding(false);
        addClassName("home-view");
        
        add(new HeaderComponent(authService));
        add(createSearchSection());
        add(createCategoriesSection());
        add(createRestaurantsSection());
        add(new FooterComponent());
        
        loadRestaurants();
    }

    private Div createSearchSection() {
        Div searchSection = new Div();
        searchSection.addClassName("search-section");
        searchSection.setWidthFull();
        
        Div searchContent = new Div();
        searchContent.addClassName("search-content");
        
        H2 title = new H2("Zamów jedzenie z ulubionych restauracji");
        title.addClassName("search-title");
        
        // City selector with location button
        Div citySelectorWrapper = new Div();
        citySelectorWrapper.addClassName("city-selector-wrapper");
        
        HorizontalLayout cityLayout = new HorizontalLayout();
        cityLayout.addClassName("city-layout");
        cityLayout.setSpacing(true);
        cityLayout.setAlignItems(Alignment.CENTER);
        cityLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        
        cityComboBox = new ComboBox<>("Wybierz miasto");
        cityComboBox.setItems(AVAILABLE_CITIES);
        cityComboBox.setPlaceholder("Wszystkie miasta");
        cityComboBox.setClearButtonVisible(true);
        cityComboBox.setWidth("250px");
        cityComboBox.addClassName("city-selector");
        cityComboBox.addValueChangeListener(e -> {
            selectedCity = e.getValue();
            filterRestaurants();
        });
        
        Button locationBtn = new Button(VaadinIcon.LOCATION_ARROW.create());
        locationBtn.addClassName("location-btn");
        locationBtn.addThemeVariants(ButtonVariant.LUMO_ICON);
        locationBtn.setTooltipText("Użyj mojej lokalizacji");
        locationBtn.addClickListener(e -> useCurrentLocation());
        locationBtn.getStyle().set("display", "flex");
        locationBtn.getStyle().set("align-items", "center");
        locationBtn.getStyle().set("justify-content", "center");
        
        cityLayout.add(cityComboBox, locationBtn);
        citySelectorWrapper.add(cityLayout);
        
        // Search wrapper
        Div searchWrapper = new Div();
        searchWrapper.addClassName("search-wrapper");
        
        searchField = new TextField();
        searchField.setPlaceholder("Szukaj restauracji...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.addClassName("search-input");
        searchField.addValueChangeListener(e -> filterRestaurants());
        
        searchWrapper.add(searchField);
        searchContent.add(title, citySelectorWrapper, searchWrapper);
        searchSection.add(searchContent);
        
        return searchSection;
    }

    private void useCurrentLocation() {
        getElement().executeJs(
            "if (navigator.geolocation) { " +
            "  navigator.geolocation.getCurrentPosition(" +
            "    function(position) { " +
            "      const lat = position.coords.latitude; " +
            "      const lng = position.coords.longitude; " +
            "      $0.$server.onLocationReceived(lat, lng); " +
            "    }, " +
            "    function(error) { " +
            "      $0.$server.onLocationError(error.message); " +
            "    }" +
            "  ); " +
            "} else { " +
            "  $0.$server.onLocationError('Geolocation not supported'); " +
            "}",
            getElement()
        );
    }
    
    @com.vaadin.flow.component.ClientCallable
    private void onLocationReceived(double lat, double lng) {
        // Sprawdź czy użytkownik jest w jednym z dostępnych miast
        String detectedCity = detectCityFromCoordinates(lat, lng);
        
        if (detectedCity != null && AVAILABLE_CITIES.contains(detectedCity)) {
            // Użytkownik jest w dostępnym mieście - ustaw miasto w selektorze
            cityComboBox.setValue(detectedCity);
            selectedCity = detectedCity;
            filterRestaurants();
            Notification.show("Wykryto lokalizację: " + detectedCity, 2000, Notification.Position.TOP_CENTER);
        } else {
            // Użytkownik nie jest w dostępnym mieście
            showLocationNotAvailableDialog();
        }
    }
    
    private String detectCityFromCoordinates(double lat, double lng) {
        // Przybliżone współrzędne miast (środek miasta)
        // Warszawa: 52.2297° N, 21.0122° E
        // Lublin: 51.2465° N, 22.5684° E
        // Rzeszów: 50.0413° N, 21.9990° E
        
        // Zakres tolerancji: ±0.5 stopnia (około 55 km)
        double tolerance = 0.5;
        
        // Sprawdź Warszawa
        if (Math.abs(lat - 52.2297) < tolerance && Math.abs(lng - 21.0122) < tolerance) {
            return "Warszawa";
        }
        
        // Sprawdź Lublin
        if (Math.abs(lat - 51.2465) < tolerance && Math.abs(lng - 22.5684) < tolerance) {
            return "Lublin";
        }
        
        // Sprawdź Rzeszów
        if (Math.abs(lat - 50.0413) < tolerance && Math.abs(lng - 21.9990) < tolerance) {
            return "Rzeszów";
        }
        
        return null;
    }
    
    @com.vaadin.flow.component.ClientCallable
    private void onLocationError(String error) {
        Notification.show("Nie można uzyskać lokalizacji. Sprawdź ustawienia przeglądarki.", 3000, Notification.Position.MIDDLE);
    }

    private void showLocationNotAvailableDialog() {
        Dialog dialog = new Dialog();
        dialog.addClassName("location-dialog");
        dialog.setHeaderTitle("Przepraszamy");
        
        VerticalLayout content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(true);
        content.getStyle().set("background-color", "var(--bg-primary)");
        
        Paragraph message = new Paragraph(
            "Obecnie nie dostarczamy w Twojej lokalizacji. " +
            "Dostępne miasta to: Warszawa, Lublin, Rzeszów."
        );
        message.addClassName(LumoUtility.TextAlignment.CENTER);
        message.getStyle().set("color", "var(--text-primary)");
        
        Button closeBtn = new Button("Rozumiem", e -> dialog.close());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        content.add(message);
        dialog.getFooter().add(closeBtn);
        dialog.add(content);
        dialog.open();
    }

    private Div createCategoriesSection() {
        Div section = new Div();
        section.addClassName("categories-section");
        section.setWidthFull();
        
        Div sectionContent = new Div();
        sectionContent.addClassName("section-content");
        
        H3 sectionTitle = new H3("Kategorie");
        sectionTitle.addClassName("section-title");
        
        categoriesContainer = new Div();
        categoriesContainer.addClassName("categories-container");
        categoriesContainer.setWidthFull();
        categoriesContainer.getStyle().set("display", "flex");
        categoriesContainer.getStyle().set("justify-content", "center");
        categoriesContainer.getStyle().set("flex-wrap", "wrap");
        
        // Dodaj kategorię "Wszystkie"
        Button allCategoryBtn = createCategoryButton("Wszystkie", null);
        allCategoryBtn.addClassName("category-active");
        categoriesContainer.add(allCategoryBtn);
        
        // Dodaj kategorie z ikonami
        for (Map.Entry<String, String> entry : CATEGORY_ICONS.entrySet()) {
            Button categoryBtn = createCategoryButton(entry.getValue() + " " + entry.getKey(), entry.getKey());
            categoriesContainer.add(categoryBtn);
        }
        
        sectionContent.add(sectionTitle, categoriesContainer);
        section.add(sectionContent);
        
        return section;
    }

    private Button createCategoryButton(String label, String category) {
        Button btn = new Button(label);
        btn.addClassName("category-btn");
        btn.addClickListener(e -> {
            selectedCategory = category;
            // Usuń aktywną klasę ze wszystkich przycisków
            categoriesContainer.getChildren()
                .forEach(child -> child.getElement().getClassList().remove("category-active"));
            // Dodaj aktywną klasę do klikniętego przycisku
            btn.addClassName("category-active");
            filterRestaurants();
        });
        return btn;
    }

    private Div createRestaurantsSection() {
        Div section = new Div();
        section.addClassName("restaurants-section");
        section.setWidthFull();
        
        Div sectionContent = new Div();
        sectionContent.addClassName("section-content");
        
        H3 sectionTitle = new H3("Dostępne restauracje");
        sectionTitle.addClassName("section-title");
        
        restaurantsContainer = new Div();
        restaurantsContainer.addClassName("restaurants-container");
        restaurantsContainer.setWidthFull();
        
        sectionContent.add(sectionTitle, restaurantsContainer);
        section.add(sectionContent);
        
        return section;
    }

    private Div createRestaurantCard(RestaurantSummaryDto restaurant) {
        Div card = new Div();
        card.addClassName("restaurant-card");
        card.addClassName("fade-in");
        
        // Image
        Div imageDiv = new Div();
        imageDiv.addClassName("restaurant-image");
        if (restaurant.imageUrl() != null && !restaurant.imageUrl().isEmpty()) {
            Image img = new Image(restaurant.imageUrl(), restaurant.name());
            img.addClassName("restaurant-img");
            imageDiv.add(img);
        } else {
            // Fallback do emoji jeśli brak zdjęcia
            imageDiv.setText(getRestaurantEmoji(restaurant.name()));
        }
        
        // Content
        Div content = new Div();
        content.addClassName("restaurant-content");
        
        H4 name = new H4(restaurant.name());
        name.addClassName("restaurant-name");
        
        Paragraph address = new Paragraph(restaurant.address());
        address.addClassName("restaurant-address");
        
        // Delivery price
        Span deliveryInfo = new Span("💰 " + String.format("%.2f zł", restaurant.deliveryPrice()) + " dostawa");
        deliveryInfo.addClassName("delivery-price");
        
        // Menu button - wyśrodkowany pod ceną dostawy
        Div buttonWrapper = new Div();
        buttonWrapper.addClassName("menu-button-wrapper");
        Button viewMenuBtn = new Button("Zobacz menu", VaadinIcon.MENU.create());
        viewMenuBtn.addClassName("view-menu-btn");
        viewMenuBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        viewMenuBtn.addClickListener(e -> showMenuDialog(restaurant));
        buttonWrapper.add(viewMenuBtn);
        
        content.add(name, address, deliveryInfo, buttonWrapper);
        card.add(imageDiv, content);
        
        return card;
    }

    private String getRestaurantEmoji(String name) {
        String lower = name.toLowerCase();
        if (lower.contains("pizza")) return "🍕";
        if (lower.contains("burger")) return "🍔";
        if (lower.contains("sushi")) return "🍣";
        if (lower.contains("taco") || lower.contains("mexican")) return "🌮";
        if (lower.contains("kebab")) return "🌯";
        if (lower.contains("asian")) return "🍜";
        if (lower.contains("italian")) return "🍝";
        return "🍽️";
    }

    private void showMenuDialog(RestaurantSummaryDto restaurant) {
        Dialog dialog = new Dialog();
        dialog.addClassName("menu-dialog");
        dialog.setHeaderTitle(restaurant.name() + " - Menu");
        dialog.setWidth("90vw");
        dialog.setMaxWidth("900px");
        dialog.setMaxHeight("85vh");
        
        VerticalLayout content = new VerticalLayout();
        content.addClassName("menu-content");
        content.setPadding(true);
        content.setSpacing(true);
        
        try {
            List<DishDto> menu = restaurantService.getMenu(restaurant.id());
            
            if (menu.isEmpty()) {
                Paragraph emptyMsg = new Paragraph("Brak dostępnych dań w menu.");
                emptyMsg.addClassName(LumoUtility.TextAlignment.CENTER);
                content.add(emptyMsg);
            } else {
                for (DishDto dish : menu) {
                    if (dish.available()) {
                        content.add(createDishCard(dish));
                    }
                }
            }
        } catch (Exception e) {
            Notification.show("Błąd podczas ładowania menu: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }
        
        Button closeBtn = new Button("Zamknij", e -> dialog.close());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        closeBtn.addClassName("close-btn");
        
        dialog.getFooter().add(closeBtn);
        dialog.add(content);
        dialog.open();
    }

    private Div createDishCard(DishDto dish) {
        Div card = new Div();
        card.addClassName("dish-card");
        card.addClassName("fade-in");
        
        // Header z nazwą i kategorią
        Div cardHeader = new Div();
        cardHeader.addClassName("dish-card-header");
        
        Div nameCategoryRow = new Div();
        nameCategoryRow.addClassName("dish-name-category-row");
        
        H4 name = new H4(dish.name());
        name.addClassName("dish-name");
        
        if (dish.category() != null && !dish.category().isEmpty()) {
            Span category = new Span(CATEGORY_ICONS.getOrDefault(dish.category(), "🍽️") + " " + dish.category());
            category.addClassName("dish-category");
            nameCategoryRow.add(name, category);
        } else {
            nameCategoryRow.add(name);
        }
        
        cardHeader.add(nameCategoryRow);
        
        // Opis
        Paragraph description = new Paragraph(dish.description() != null ? dish.description() : "");
        description.addClassName("dish-description");
        
        // Footer z ceną i przyciskiem
        Div cardFooter = new Div();
        cardFooter.addClassName("dish-card-footer");
        
        Span price = new Span(String.format("%.2f zł", dish.price()));
        price.addClassName("dish-price");
        
        Button addToCartBtn = new Button("Dodaj do koszyka", VaadinIcon.CART.create());
        addToCartBtn.addClassName("add-to-cart-btn");
        addToCartBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        addToCartBtn.addClickListener(e -> {
            Notification.show("Dodano " + dish.name() + " do koszyka", 2000, Notification.Position.TOP_CENTER);
        });
        
        cardFooter.add(price, addToCartBtn);
        
        card.add(cardHeader, description, cardFooter);
        return card;
    }

    private void loadRestaurants() {
        try {
            allRestaurants = restaurantService.listRestaurants();
            updateRestaurantsDisplay(allRestaurants);
            
            if (allRestaurants.isEmpty()) {
                Paragraph emptyMsg = new Paragraph("Brak dostępnych restauracji.");
                emptyMsg.addClassName(LumoUtility.TextAlignment.CENTER);
                emptyMsg.addClassName(LumoUtility.Margin.LARGE);
                restaurantsContainer.removeAll();
                restaurantsContainer.add(emptyMsg);
            }
        } catch (Exception e) {
            Notification.show("Błąd podczas ładowania restauracji: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }

    private void filterRestaurants() {
        String searchTerm = searchField.getValue();
        
        List<RestaurantSummaryDto> filtered = allRestaurants.stream()
                .filter(r -> {
                    // Filtruj po mieście
                    if (selectedCity != null && !selectedCity.isEmpty()) {
                        String address = r.address().toLowerCase();
                        if (!address.contains(selectedCity.toLowerCase())) {
                            return false;
                        }
                    }
                    
                    // Filtruj po kategorii - sprawdź czy restauracja ma dania w tej kategorii
                    if (selectedCategory != null && !selectedCategory.isEmpty()) {
                        try {
                            List<DishDto> menu = restaurantService.getMenu(r.id());
                            boolean hasCategory = menu.stream()
                                    .anyMatch(d -> selectedCategory.equals(d.category()) && d.available());
                            if (!hasCategory) {
                                return false;
                            }
                        } catch (Exception e) {
                            return false;
                        }
                    }
                    
                    // Filtruj po wyszukiwaniu
                    if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                        String lowerTerm = searchTerm.toLowerCase();
                        return r.name().toLowerCase().contains(lowerTerm) ||
                               r.address().toLowerCase().contains(lowerTerm);
                    }
                    
                    return true;
                })
                .collect(Collectors.toList());
        
        updateRestaurantsDisplay(filtered);
    }
    
    private void updateRestaurantsDisplay(List<RestaurantSummaryDto> restaurants) {
        if (restaurantsContainer != null) {
            restaurantsContainer.removeAll();
            
            if (restaurants.isEmpty()) {
                Paragraph emptyMsg = new Paragraph("Nie znaleziono restauracji spełniających kryteria.");
                emptyMsg.addClassName(LumoUtility.TextAlignment.CENTER);
                emptyMsg.addClassName(LumoUtility.Margin.LARGE);
                restaurantsContainer.add(emptyMsg);
                return;
            }
            
            int delay = 0;
            for (RestaurantSummaryDto restaurant : restaurants) {
                Div card = createRestaurantCard(restaurant);
                card.getElement().getStyle().set("animation-delay", delay + "ms");
                restaurantsContainer.add(card);
                delay += 50;
            }
        }
    }
}
