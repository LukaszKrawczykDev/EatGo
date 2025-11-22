package pollub.eatgo.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import pollub.eatgo.dto.dish.DishDto;
import pollub.eatgo.dto.restaurant.RestaurantSummaryDto;
import pollub.eatgo.service.AuthenticationService;
import pollub.eatgo.service.RestaurantService;
import pollub.eatgo.service.TokenValidationService;
import pollub.eatgo.views.components.HeaderComponent;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Route("restaurant-view")
@PageTitle("EatGo - Restauracja")
public class RestaurantView extends VerticalLayout implements HasUrlParameter<String> {
    
    private final RestaurantService restaurantService;
    private final AuthenticationService authService;
    private final TokenValidationService tokenValidationService;
    private Long restaurantId;
    private RestaurantSummaryDto restaurant;
    private List<DishDto> menu;
    private HeaderComponent headerComponent;
    
    public RestaurantView(RestaurantService restaurantService, AuthenticationService authService, TokenValidationService tokenValidationService) {
        this.restaurantService = restaurantService;
        this.authService = authService;
        this.tokenValidationService = tokenValidationService;
        
        setSizeFull();
        setSpacing(false);
        setPadding(false);
        addClassName("restaurant-view");
    }
    
    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        try {
            restaurantId = Long.parseLong(parameter);
            loadRestaurantData();
        } catch (NumberFormatException e) {
            Notification.show("Nieprawidłowy identyfikator restauracji", 3000, Notification.Position.TOP_CENTER);
            event.forwardTo(HomeView.class);
        }
    }
    
    private void loadRestaurantData() {
        // Pobierz dane restauracji i menu
        List<RestaurantSummaryDto> restaurants = restaurantService.listRestaurants();
        restaurant = restaurants.stream()
                .filter(r -> r.id().equals(restaurantId))
                .findFirst()
                .orElse(null);
        
        if (restaurant == null) {
            Notification.show("Restauracja nie została znaleziona", 3000, Notification.Position.TOP_CENTER);
            getUI().ifPresent(ui -> ui.navigate(HomeView.class));
            return;
        }
        
        menu = restaurantService.getMenu(restaurantId);
        System.out.println("RestaurantView: Loaded menu for restaurant " + restaurantId + ", items count: " + (menu != null ? menu.size() : 0));
        if (menu != null && !menu.isEmpty()) {
            menu.forEach(dish -> System.out.println("  - " + dish.name() + " (category: " + dish.category() + ", available: " + dish.available() + ")"));
        }
        buildView();
    }
    
    private void buildView() {
        removeAll();
        
        System.out.println("RestaurantView.buildView: Starting build, menu size: " + (menu != null ? menu.size() : "null"));
        
        // Header
        headerComponent = new HeaderComponent(authService, tokenValidationService);
        add(headerComponent);
        System.out.println("RestaurantView.buildView: Header added");
        
        // Hero section
        Div heroSection = createHeroSection();
        add(heroSection);
        System.out.println("RestaurantView.buildView: Hero section added");
        
        // Menu section
        Div menuSectionDiv;
        if (menu != null && !menu.isEmpty()) {
            System.out.println("RestaurantView.buildView: Creating menu section with " + menu.size() + " dishes");
            menuSectionDiv = createMenuSection();
        } else {
            System.out.println("RestaurantView.buildView: Menu is empty, creating empty message");
            menuSectionDiv = new Div();
            menuSectionDiv.addClassName("empty-menu");
            menuSectionDiv.setText("Brak dostępnych dań w menu.");
        }
        add(menuSectionDiv);
        System.out.println("RestaurantView.buildView: Menu section added, total children: " + getChildren().count());
    }
    
    private Div createHeroSection() {
        Div hero = new Div();
        hero.addClassName("restaurant-hero");
        
        if (restaurant.imageUrl() != null && !restaurant.imageUrl().isEmpty()) {
            Image img = new Image(restaurant.imageUrl(), restaurant.name());
            img.addClassName("restaurant-hero-image");
            hero.add(img);
        }
        
        Div heroContent = new Div();
        heroContent.addClassName("restaurant-hero-content");
        
        H1 name = new H1(restaurant.name());
        name.addClassName("restaurant-hero-name");
        
        Paragraph address = new Paragraph(restaurant.address());
        address.addClassName("restaurant-hero-address");
        
        Span deliveryInfo = new Span("💰 " + String.format("%.2f zł", restaurant.deliveryPrice()) + " dostawa");
        deliveryInfo.addClassName("restaurant-hero-delivery");
        
        heroContent.add(name, address, deliveryInfo);
        hero.add(heroContent);
        
        return hero;
    }
    
    private Div createMenuSection() {
        Div menuSection = new Div();
        menuSection.addClassName("restaurant-menu-section");
        
        if (menu == null || menu.isEmpty()) {
            System.out.println("RestaurantView: Menu is null or empty!");
            Div emptyMsg = new Div();
            emptyMsg.addClassName("empty-menu");
            emptyMsg.setText("Brak dostępnych dań w menu.");
            menuSection.add(emptyMsg);
            return menuSection;
        }
        
        System.out.println("RestaurantView: Creating menu section with " + menu.size() + " dishes");
        
        // Grupuj dania po kategoriach (wszystkie dania z getMenu są już dostępne)
        Map<String, List<DishDto>> dishesByCategory = menu.stream()
                .collect(Collectors.groupingBy(
                        dish -> dish.category() != null && !dish.category().isEmpty() ? dish.category() : "Inne"
                ));
        
        System.out.println("RestaurantView: Dishes grouped into " + dishesByCategory.size() + " categories");
        
        if (dishesByCategory.isEmpty()) {
            Div emptyMsg = new Div();
            emptyMsg.addClassName("empty-menu");
            emptyMsg.setText("Brak dostępnych dań w menu.");
            menuSection.add(emptyMsg);
            return menuSection;
        }
        
        for (Map.Entry<String, List<DishDto>> entry : dishesByCategory.entrySet()) {
            String category = entry.getKey();
            List<DishDto> dishes = entry.getValue();
            
            System.out.println("RestaurantView: Adding category '" + category + "' with " + dishes.size() + " dishes");
            
            Div categorySection = new Div();
            categorySection.addClassName("menu-category-section");
            
            H2 categoryTitle = new H2(category);
            categoryTitle.addClassName("menu-category-title");
            categorySection.add(categoryTitle);
            
            Div dishesContainer = new Div();
            dishesContainer.addClassName("dishes-container");
            
            for (DishDto dish : dishes) {
                System.out.println("RestaurantView: Adding dish card for " + dish.name());
                Div dishCard = createDishCard(dish);
                dishesContainer.add(dishCard);
                System.out.println("RestaurantView: Dish card added, children count: " + dishesContainer.getChildren().count());
            }
            
            System.out.println("RestaurantView: Dishes container has " + dishesContainer.getChildren().count() + " children");
            categorySection.add(dishesContainer);
            System.out.println("RestaurantView: Category section has " + categorySection.getChildren().count() + " children");
            menuSection.add(categorySection);
            System.out.println("RestaurantView: Menu section has " + menuSection.getChildren().count() + " children");
        }
        
        System.out.println("RestaurantView: Final menu section has " + menuSection.getChildren().count() + " children");
        return menuSection;
    }
    
    private Div createDishCard(DishDto dish) {
        System.out.println("RestaurantView.createDishCard: Creating card for " + dish.name());
        Div card = new Div();
        card.addClassName("dish-card");
        card.getStyle().set("display", "block"); // Wymuś wyświetlenie
        card.getStyle().set("visibility", "visible");
        card.getStyle().set("opacity", "1");
        
        // Image - zawsze wyświetlaj, nawet jeśli brak URL (placeholder)
        Div imageDiv = new Div();
        imageDiv.addClassName("dish-image");
        
        if (dish.imageUrl() != null && !dish.imageUrl().isEmpty()) {
            Image img = new Image(dish.imageUrl(), dish.name());
            img.addClassName("dish-img");
            img.setAlt(dish.name());
            imageDiv.add(img);
            System.out.println("RestaurantView.createDishCard: Added image for " + dish.name() + " from " + dish.imageUrl());
        } else {
            // Placeholder jeśli brak zdjęcia - użyj emoji lub ikony
            Div placeholder = new Div();
            placeholder.addClassName("dish-image-placeholder");
            placeholder.setText("🍽️");
            placeholder.getStyle().set("display", "flex");
            placeholder.getStyle().set("align-items", "center");
            placeholder.getStyle().set("justify-content", "center");
            placeholder.getStyle().set("font-size", "4rem");
            placeholder.getStyle().set("color", "var(--text-secondary)");
            imageDiv.add(placeholder);
            System.out.println("RestaurantView.createDishCard: Added placeholder for " + dish.name() + " (no image URL)");
        }
        card.add(imageDiv);
        
        Div content = new Div();
        content.addClassName("dish-card-content");
        
        H3 name = new H3(dish.name());
        name.addClassName("dish-name");
        name.getStyle().set("display", "block");
        
        Paragraph description = new Paragraph(dish.description() != null ? dish.description() : "");
        description.addClassName("dish-description");
        description.getStyle().set("display", "block");
        
        HorizontalLayout bottom = new HorizontalLayout();
        bottom.addClassName("dish-card-bottom");
        bottom.setWidthFull();
        bottom.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);
        bottom.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        
        Span price = new Span(String.format("%.2f zł", dish.price()));
        price.addClassName("dish-price");
        price.getStyle().set("display", "inline-block");
        
        Button addButton = new Button("Dodaj", VaadinIcon.PLUS.create());
        addButton.addClassName("add-dish-btn");
        addButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        addButton.addClickListener(e -> addToCart(dish));
        addButton.setEnabled(dish.available());
        
        bottom.add(price, addButton);
        content.add(name, description, bottom);
        card.add(content);
        
        System.out.println("RestaurantView.createDishCard: Card created with " + card.getChildren().count() + " children");
        return card;
    }
    
    private void addToCart(DishDto dish) {
        System.out.println("RestaurantView.addToCart: Adding dish " + dish.name() + " (ID: " + dish.id() + ") to restaurant " + restaurantId);
        
        // Pobierz koszyki z localStorage - konwertuj Long na String/Number dla JavaScript
        String restaurantIdStr = String.valueOf(restaurantId);
        String dishIdStr = String.valueOf(dish.id());
        String dishName = dish.name();
        String dishPriceStr = String.valueOf(dish.price());
        
        getElement().executeJs(
            "const carts = JSON.parse(localStorage.getItem('eatgo-carts') || '{}'); " +
            "const restaurantId = $0; " +
            "const dishId = Number($1); " +
            "const dishName = $2; " +
            "const dishPrice = Number($3); " +
            "" +
            "console.log('Adding to cart - restaurantId:', restaurantId, 'dishId:', dishId, 'name:', dishName, 'price:', dishPrice); " +
            "" +
            "if (!carts[restaurantId]) { " +
            "  carts[restaurantId] = { items: [] }; " +
            "  console.log('Created new cart for restaurant:', restaurantId); " +
            "} " +
            "" +
            "const existingItem = carts[restaurantId].items.find(item => item.dishId === dishId); " +
            "if (existingItem) { " +
            "  existingItem.quantity += 1; " +
            "  console.log('Increased quantity for existing item:', existingItem); " +
            "} else { " +
            "  const newItem = { " +
            "    dishId: dishId, " +
            "    name: dishName, " +
            "    price: dishPrice, " +
            "    quantity: 1 " +
            "  }; " +
            "  carts[restaurantId].items.push(newItem); " +
            "  console.log('Added new item to cart:', newItem); " +
            "} " +
            "" +
            "localStorage.setItem('eatgo-carts', JSON.stringify(carts)); " +
            "console.log('Cart saved to localStorage:', carts); " +
            "window.dispatchEvent(new Event('eatgo-cart-changed')); " +
            "return true;",
            restaurantIdStr, dishIdStr, dishName, dishPriceStr
        ).then(Boolean.class, success -> {
            if (success != null && success) {
                Notification.show("Dodano do koszyka: " + dish.name(), 2000, Notification.Position.TOP_CENTER);
            } else {
                Notification.show("Błąd podczas dodawania do koszyka", 3000, Notification.Position.TOP_CENTER);
            }
        });
    }
}

