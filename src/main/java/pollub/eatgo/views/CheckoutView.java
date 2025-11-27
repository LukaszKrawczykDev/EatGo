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
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import pollub.eatgo.dto.address.AddressCreateDto;
import pollub.eatgo.dto.address.AddressDto;
import pollub.eatgo.dto.order.OrderCreateRequestDto;
import pollub.eatgo.dto.order.OrderItemRequestDto;
import pollub.eatgo.dto.restaurant.RestaurantSummaryDto;
import pollub.eatgo.service.AddressService;
import pollub.eatgo.service.AuthenticationService;
import pollub.eatgo.service.OrderNotificationService;
import pollub.eatgo.service.RestaurantService;
import pollub.eatgo.service.TokenValidationService;
import pollub.eatgo.views.components.HeaderComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Route("checkout")
@PageTitle("EatGo - Finalizacja zamówienia")
public class CheckoutView extends VerticalLayout implements HasUrlParameter<String> {
    
    private final RestaurantService restaurantService;
    private final AuthenticationService authService;
    private final TokenValidationService tokenValidationService;
    private final AddressService addressService;
    private final OrderNotificationService orderNotificationService;
    private Long restaurantId;
    private RestaurantSummaryDto restaurant;
    private Div itemsContainer;
    private Span totalSpan;
    private ComboBox<AddressDto> addressComboBox;
    private RadioButtonGroup<String> paymentMethodGroup;
    private List<OrderItemRequestDto> cartItems = new ArrayList<>();
    private Dialog addressDialog; // Przechowuj referencję do dialogu
    private List<AddressDto> currentAddresses = new ArrayList<>(); // Aktualna lista adresów
    
    public CheckoutView(RestaurantService restaurantService,
                        AuthenticationService authService,
                        TokenValidationService tokenValidationService,
                        AddressService addressService,
                        OrderNotificationService orderNotificationService) {
        this.restaurantService = restaurantService;
        this.authService = authService;
        this.tokenValidationService = tokenValidationService;
        this.addressService = addressService;
        this.orderNotificationService = orderNotificationService;
        
        setSizeFull();
        setSpacing(false);
        setPadding(false);
        addClassName("checkout-view");
    }
    
    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        try {
            restaurantId = Long.parseLong(parameter);
            loadCheckoutData();
        } catch (NumberFormatException e) {
            Notification.show("Nieprawidłowy identyfikator restauracji", 3000, Notification.Position.TOP_CENTER);
            event.forwardTo(CartView.class);
        }
    }
    
    private void loadCheckoutData() {
        List<RestaurantSummaryDto> restaurants = restaurantService.listRestaurants();
        restaurant = restaurants.stream()
                .filter(r -> r.id().equals(restaurantId))
                .findFirst()
                .orElse(null);
        
        if (restaurant == null) {
            Notification.show("Restauracja nie została znaleziona", 3000, Notification.Position.TOP_CENTER);
            getUI().ifPresent(ui -> ui.navigate(CartView.class));
            return;
        }
        
        buildView();
    }
    
    private void buildView() {
        removeAll();
        
        HeaderComponent header = new HeaderComponent(authService, tokenValidationService, orderNotificationService);
        add(header);
        
        Div content = new Div();
        content.addClassName("checkout-content");
        
        Button backButton = new Button("← Powrót do restauracji", VaadinIcon.ARROW_LEFT.create());
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        backButton.addClickListener(e -> {
            getUI().ifPresent(ui -> ui.navigate("restaurant-view/" + restaurantId));
        });
        
        H2 title = new H2("Finalizacja zamówienia");
        title.addClassName("checkout-title");
        
        Div productsSection = createProductsSection();
        
        Div addressSection = createAddressSection();
        
        Div paymentSection = createPaymentSection();
        
        Div summarySection = createSummarySection();
        
        content.add(backButton, title, productsSection, addressSection, paymentSection, summarySection);
        add(content);
        
        loadCartItems();
    }
    
    private Div createProductsSection() {
        Div section = new Div();
        section.addClassName("checkout-section");
        
        H3 sectionTitle = new H3("Produkty");
        sectionTitle.addClassName("checkout-section-title");
        
        itemsContainer = new Div();
        itemsContainer.addClassName("checkout-items-container");
        
        section.add(sectionTitle, itemsContainer);
        return section;
    }
    
    private Div createAddressSection() {
        Div section = new Div();
        section.addClassName("checkout-section");
        
        H3 sectionTitle = new H3("Adres dostawy");
        sectionTitle.addClassName("checkout-section-title");
        
        addressComboBox = new ComboBox<>("Wybierz adres");
        addressComboBox.setWidthFull();
        addressComboBox.setPlaceholder("Wybierz zapisany adres");
        addressComboBox.setItemLabelGenerator(address -> {
            String street = address.street();
            if (address.apartmentNumber() != null && !address.apartmentNumber().isEmpty()) {
                street += "/" + address.apartmentNumber();
            }
            return String.format("%s, %s %s", street, address.postalCode(), address.city());
        });
        
        loadUserAddresses();
        
        Button addAddressBtn = new Button("Dodaj nowy adres", VaadinIcon.PLUS.create());
        addAddressBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        addAddressBtn.addClickListener(e -> showAddAddressDialog());
        
        section.add(sectionTitle, addressComboBox, addAddressBtn);
        return section;
    }
    
    private void loadUserAddresses() {
        getElement().executeJs(
            "const userId = localStorage.getItem('eatgo-userId'); " +
            "if (userId) { " +
            "  $0.$server.loadAddressesForUser(userId); " +
            "} else { " +
            "  console.warn('CheckoutView: Brak userId w localStorage, nie można załadować adresów'); " +
            "}",
            getElement()
        );
    }
    
    @com.vaadin.flow.component.ClientCallable
    public void loadAddressesForUser(String userIdStr) {
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                try {
                    Long userId = Long.parseLong(userIdStr);
                    List<AddressDto> addresses = addressService.listAddresses(userId);
                    System.out.println("CheckoutView.loadAddressesForUser: Loaded " + addresses.size() + " addresses for user " + userId);
                    currentAddresses = new ArrayList<>(addresses);
                    addressComboBox.setItems(currentAddresses);
                    
                    getElement().executeJs(
                        "const token = localStorage.getItem('eatgo-token'); " +
                        "if (token && token !== 'null' && token !== '') { " +
                        "  fetch('/api/users/settings', { " +
                        "    method: 'GET', " +
                        "    headers: { " +
                        "      'Authorization': 'Bearer ' + token, " +
                        "      'Content-Type': 'application/json' " +
                        "    } " +
                        "  }) " +
                        "  .then(r => { " +
                        "    if (!r.ok) { " +
                        "      console.warn('Failed to load user settings'); " +
                        "      return null; " +
                        "    } " +
                        "    return r.json(); " +
                        "  }) " +
                        "  .then(settings => { " +
                        "    if (settings && settings.defaultAddressId) { " +
                        "      $0.$server.setDefaultAddress(String(settings.defaultAddressId)); " +
                        "    } else if ($1.length > 0) { " +
                        "      // Jeśli nie ma domyślnego, ustaw pierwszy adres " +
                        "      $0.$server.setDefaultAddress(null); " +
                        "    } " +
                        "  }) " +
                        "  .catch(e => { " +
                        "    console.error('Error loading user settings:', e); " +
                        "    // Fallback: ustaw pierwszy adres jeśli są dostępne " +
                        "    if ($1.length > 0) { " +
                        "      $0.$server.setDefaultAddress(null); " +
                        "    } " +
                        "  }); " +
                        "} else if ($1.length > 0) { " +
                        "  // Dla niezalogowanych, ustaw pierwszy adres " +
                        "  $0.$server.setDefaultAddress(null); " +
                        "}",
                        getElement(), addresses.size()
                    );
                } catch (Exception e) {
                    System.err.println("CheckoutView: Error loading addresses: " + e.getMessage());
                    e.printStackTrace();
                    Notification.show("Błąd podczas ładowania adresów: " + e.getMessage(), 3000, Notification.Position.TOP_CENTER);
                }
            });
        });
    }
    
    @com.vaadin.flow.component.ClientCallable
    public void setDefaultAddress(String addressIdStr) {
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                if (addressIdStr == null || addressIdStr.isEmpty() || "null".equals(addressIdStr)) {
                    if (!currentAddresses.isEmpty() && addressComboBox.getValue() == null) {
                        addressComboBox.setValue(currentAddresses.get(0));
                    }
                } else {
                    try {
                        Long addressId = Long.parseLong(addressIdStr);
                        currentAddresses.stream()
                            .filter(a -> a.id().equals(addressId))
                            .findFirst()
                            .ifPresent(addressComboBox::setValue);
                    } catch (NumberFormatException e) {
                        System.err.println("CheckoutView: Invalid address ID format: " + addressIdStr);
                        if (!currentAddresses.isEmpty() && addressComboBox.getValue() == null) {
                            addressComboBox.setValue(currentAddresses.get(0));
                        }
                    }
                }
            });
        });
    }
    
    private Div createPaymentSection() {
        Div section = new Div();
        section.addClassName("checkout-section");
        
        H3 sectionTitle = new H3("Metoda płatności");
        sectionTitle.addClassName("checkout-section-title");
        
        paymentMethodGroup = new RadioButtonGroup<>();
        paymentMethodGroup.setItems("Gotówka", "Karta", "BLIK");
        paymentMethodGroup.setValue("Gotówka");
        paymentMethodGroup.addClassName("payment-method-group");
        
        TextField paymentCodeField = new TextField("Kod");
        paymentCodeField.setWidthFull();
        paymentCodeField.setPlaceholder("Wpisz kod");
        paymentCodeField.setVisible(false);
        paymentCodeField.addClassName("payment-code-field");
        
        paymentMethodGroup.addValueChangeListener(e -> {
            String selected = e.getValue();
            paymentCodeField.setVisible("BLIK".equals(selected) || "Karta".equals(selected));
            if ("BLIK".equals(selected)) {
                paymentCodeField.setPlaceholder("Wpisz kod BLIK (6 cyfr)");
                paymentCodeField.setPrefixComponent(VaadinIcon.MOBILE.create());
            } else if ("Karta".equals(selected)) {
                paymentCodeField.setPlaceholder("Wpisz numer karty");
                paymentCodeField.setPrefixComponent(VaadinIcon.CREDIT_CARD.create());
            }
        });
        
        section.add(sectionTitle, paymentMethodGroup, paymentCodeField);
        return section;
    }
    
    private Div createSummarySection() {
        Div section = new Div();
        section.addClassName("checkout-summary-section");
        
        VerticalLayout summaryLayout = new VerticalLayout();
        summaryLayout.setSpacing(true);
        summaryLayout.setPadding(false);
        
        HorizontalLayout subtotalLayout = new HorizontalLayout();
        subtotalLayout.setWidthFull();
        subtotalLayout.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);
        subtotalLayout.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        
        Span subtotalLabel = new Span("Suma pośrednia:");
        subtotalLabel.addClassName("checkout-subtotal-label");
        
        Span subtotalSpan = new Span("0,00 zł");
        subtotalSpan.addClassName("checkout-subtotal");
        subtotalSpan.setId("checkout-subtotal-span");
        
        subtotalLayout.add(subtotalLabel, subtotalSpan);
        
        HorizontalLayout deliveryLayout = new HorizontalLayout();
        deliveryLayout.setWidthFull();
        deliveryLayout.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);
        deliveryLayout.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        
        Span deliveryLabel = new Span("Dostawa:");
        deliveryLabel.addClassName("checkout-delivery-label");
        
        Span deliveryPriceSpan = new Span(String.format("%.2f zł", restaurant.deliveryPrice() != null ? restaurant.deliveryPrice().doubleValue() : 0.0));
        deliveryPriceSpan.addClassName("checkout-delivery-price");
        deliveryPriceSpan.setId("checkout-delivery-span");
        
        deliveryLayout.add(deliveryLabel, deliveryPriceSpan);
        
        HorizontalLayout totalLayout = new HorizontalLayout();
        totalLayout.setWidthFull();
        totalLayout.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);
        totalLayout.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        totalLayout.addClassName("checkout-total-layout");
        totalLayout.getStyle().set("border-top", "1px solid var(--lumo-contrast-10pct)");
        totalLayout.getStyle().set("padding-top", "1rem");
        totalLayout.getStyle().set("margin-top", "0.5rem");
        
        Span totalLabel = new Span("Suma całkowita:");
        totalLabel.addClassName("checkout-total-label");
        totalLabel.getStyle().set("font-weight", "bold");
        
        totalSpan = new Span("0,00 zł");
        totalSpan.addClassName("checkout-total");
        totalSpan.getStyle().set("font-weight", "bold");
        totalSpan.getStyle().set("font-size", "1.25rem");
        
        totalLayout.add(totalLabel, totalSpan);
        
        summaryLayout.add(subtotalLayout, deliveryLayout, totalLayout);
        
        Button placeOrderBtn = new Button("Złóż zamówienie", VaadinIcon.CHECK.create());
        placeOrderBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        placeOrderBtn.setWidthFull();
        placeOrderBtn.addClickListener(e -> placeOrder());
        
        section.add(summaryLayout, placeOrderBtn);
        return section;
    }
    
    private void loadCartItems() {
        double deliveryPrice = restaurant.deliveryPrice() != null ?
            restaurant.deliveryPrice().doubleValue() : 0.0;
        
        getElement().executeJs(
            "const carts = JSON.parse(localStorage.getItem('eatgo-carts') || '{}'); " +
            "const cart = carts[$0]; " +
            "if (cart && cart.items) { " +
            "  $1.$server.displayCartItems(JSON.stringify(cart.items), $2); " +
            "} else { " +
            "  $1.$server.displayCartItems('[]', $2); " +
            "}",
            String.valueOf(restaurantId), getElement(), deliveryPrice
        );
    }
    
    @com.vaadin.flow.component.ClientCallable
    public void displayCartItems(String itemsJson, double deliveryPrice) {
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                itemsContainer.removeAll();
                
                System.out.println("CheckoutView: displayCartItems called with itemsJson: " + itemsJson);
                
                if (itemsJson == null || itemsJson.isEmpty() || "[]".equals(itemsJson)) {
                    Div emptyMsg = new Div();
                    emptyMsg.addClassName("empty-cart");
                    emptyMsg.setText("Koszyk jest pusty");
                    itemsContainer.add(emptyMsg);
                    return;
                }
                
                try {
                    cartItems.clear();
                    
                    getElement().executeJs(
                        "const items = JSON.parse($0); " +
                        "console.log('CheckoutView: Parsed items:', items); " +
                        "let total = 0; " +
                        "if (items && items.length > 0) { " +
                        "  items.forEach((item, index) => { " +
                        "    total += item.price * item.quantity; " +
                        "    $1.$server.addCartItem(String(item.dishId), item.name, String(item.price), String(item.quantity), index); " +
                        "  }); " +
                        "} " +
                        "$1.$server.updateTotal(total, $2);",
                        itemsJson, getElement(), deliveryPrice
                    );
                } catch (Exception e) {
                    System.err.println("CheckoutView: Error displaying cart items: " + e.getMessage());
                    e.printStackTrace();
                    Notification.show("Błąd podczas wyświetlania produktów", 3000, Notification.Position.TOP_CENTER);
                }
            });
        });
    }
    
    @com.vaadin.flow.component.ClientCallable
    public void addCartItem(String dishIdStr, String name, String priceStr, String quantityStr, int index) {
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                try {
                    Long dishId = Long.parseLong(dishIdStr);
                    double price = Double.parseDouble(priceStr);
                    int quantity = Integer.parseInt(quantityStr);
                    
                    cartItems.add(new OrderItemRequestDto(dishId, quantity));
                    
                    Div itemCard = createCartItemCard(dishId, name, price, quantity);
                    itemsContainer.add(itemCard);
                } catch (NumberFormatException e) {
                    System.err.println("CheckoutView: Error parsing cart item: " + e.getMessage());
                }
            });
        });
    }
    
    private Div createCartItemCard(Long dishId, String name, double price, int quantity) {
        Div card = new Div();
        card.addClassName("checkout-item-card");
        
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);
        layout.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        
        Div itemInfo = new Div();
        itemInfo.addClassName("checkout-item-info");
        
        H4 itemName = new H4(name);
        itemName.addClassName("checkout-item-name");
        
        Span itemPrice = new Span(String.format("%.2f zł", price));
        itemPrice.addClassName("checkout-item-price");
        itemPrice.getStyle().set("font-size", "0.875rem");
        itemPrice.getStyle().set("color", "var(--lumo-secondary-text-color)");
        
        itemInfo.add(itemName, itemPrice);
        
        HorizontalLayout quantityControls = new HorizontalLayout();
        quantityControls.addClassName("checkout-quantity-controls");
        quantityControls.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        quantityControls.setSpacing(true);
        
        Button decreaseBtn = new Button("-");
        decreaseBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        decreaseBtn.addClickListener(e -> updateQuantity(dishId, quantity - 1));
        
        Span quantitySpan = new Span(String.valueOf(quantity));
        quantitySpan.addClassName("checkout-quantity");
        
        Button increaseBtn = new Button("+");
        increaseBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        increaseBtn.addClickListener(e -> updateQuantity(dishId, quantity + 1));
        
        Button removeBtn = new Button(VaadinIcon.TRASH.create());
        removeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR);
        removeBtn.addClickListener(e -> removeItem(dishId));
        
        double totalPrice = price * quantity;
        Span totalPriceSpan = new Span(String.format("%.2f zł", totalPrice));
        totalPriceSpan.addClassName("checkout-item-total-price");
        totalPriceSpan.getStyle().set("font-weight", "bold");
        totalPriceSpan.getStyle().set("font-size", "1rem");
        totalPriceSpan.getStyle().set("margin-right", "1rem");
        
        quantityControls.add(decreaseBtn, quantitySpan, increaseBtn, removeBtn, totalPriceSpan);
        
        layout.add(itemInfo, quantityControls);
        card.add(layout);
        
        return card;
    }
    
    private void updateQuantity(Long dishId, int newQuantity) {
        if (newQuantity <= 0) {
            removeItem(dishId);
            return;
        }
        
        getElement().executeJs(
            "const carts = JSON.parse(localStorage.getItem('eatgo-carts') || '{}'); " +
            "const restaurantId = String($0); " +
            "const dishId = String($1); " +
            "const cart = carts[restaurantId]; " +
            "if (cart && cart.items) { " +
            "  const item = cart.items.find(i => String(i.dishId) === dishId); " +
            "  if (item) { " +
            "    item.quantity = $2; " +
            "    localStorage.setItem('eatgo-carts', JSON.stringify(carts)); " +
            "    window.dispatchEvent(new Event('eatgo-cart-changed')); " +
            "    $3.$server.refreshCartItems(); " +
            "  } " +
            "}",
            String.valueOf(restaurantId), String.valueOf(dishId), newQuantity, getElement()
        );
    }
    
    private void removeItem(Long dishId) {
        getElement().executeJs(
            "const carts = JSON.parse(localStorage.getItem('eatgo-carts') || '{}'); " +
            "const restaurantId = String($0); " +
            "const dishId = String($1); " +
            "const cart = carts[restaurantId]; " +
            "if (cart && cart.items) { " +
            "  cart.items = cart.items.filter(i => String(i.dishId) !== dishId); " +
            "  if (cart.items.length === 0) { " +
            "    delete carts[restaurantId]; " +
            "  } else { " +
            "    carts[restaurantId] = cart; " +
            "  } " +
            "  localStorage.setItem('eatgo-carts', JSON.stringify(carts)); " +
            "  window.dispatchEvent(new Event('eatgo-cart-changed')); " +
            "  $2.$server.refreshCartItems(); " +
            "}",
            String.valueOf(restaurantId), String.valueOf(dishId), getElement()
        );
    }
    
    @com.vaadin.flow.component.ClientCallable
    public void refreshCartItems() {
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                // Wyczyść listę items i cartItems
                itemsContainer.removeAll();
                cartItems.clear();
                // Załaduj ponownie z localStorage
                loadCartItems();
            });
        });
    }
    
    @com.vaadin.flow.component.ClientCallable
    public void updateTotal(double subtotal, double deliveryPrice) {
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                double totalWithDelivery = subtotal + deliveryPrice;
                
                getElement().executeJs(
                        "const span = document.getElementById('checkout-subtotal-span');" +
                        "if (span) { span.textContent = $0 + ' zł'; }",
                        String.format("%.2f", subtotal)
                );
                
                getElement().executeJs(
                        "const span = document.getElementById('checkout-delivery-span');" +
                        "if (span) { span.textContent = $0 + ' zł'; }",
                        String.format("%.2f", deliveryPrice)
                );
                
                totalSpan.setText(String.format("%.2f zł", totalWithDelivery));
            });
        });
    }
    
    private void showAddAddressDialog() {
        addressDialog = new Dialog();
        addressDialog.setHeaderTitle("Dodaj nowy adres");
        addressDialog.setModal(true);
        addressDialog.setCloseOnEsc(true);
        addressDialog.setCloseOnOutsideClick(false);
        
        TextField streetField = new TextField("Ulica");
        streetField.setWidthFull();
        
        ComboBox<String> cityField = new ComboBox<>("Miasto");
        cityField.setWidthFull();
        cityField.setItems("Warszawa", "Lublin", "Rzeszów");
        
        TextField postalCodeField = new TextField("Kod pocztowy");
        postalCodeField.setWidthFull();
        
        TextField apartmentNumberField = new TextField("Numer mieszkania/piętro (opcjonalnie)");
        apartmentNumberField.setWidthFull();
        apartmentNumberField.setPlaceholder("np. 5/12 lub 3. piętro");
        
        TextArea notesField = new TextArea("Uwagi (opcjonalnie)");
        notesField.setWidthFull();
        
        VerticalLayout form = new VerticalLayout(streetField, cityField, postalCodeField, apartmentNumberField, notesField);
        form.setSpacing(true);
        form.setPadding(false);
        
        Button saveBtn = new Button("Zapisz");
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBtn.addClickListener(e -> {
            String street = streetField.getValue();
            String city = cityField.getValue();
            String postalCode = postalCodeField.getValue();
            String apartmentNumber = apartmentNumberField.getValue();
            
            if (street == null || street.trim().isEmpty()) {
                Notification.show("Ulica jest wymagana.", 3000, Notification.Position.TOP_CENTER);
                streetField.focus();
                return;
            }
            
            if (city == null || city.trim().isEmpty()) {
                Notification.show("Miasto jest wymagane.", 3000, Notification.Position.TOP_CENTER);
                cityField.focus();
                return;
            }
            
            if (postalCode == null || postalCode.trim().isEmpty()) {
                Notification.show("Kod pocztowy jest wymagany.", 3000, Notification.Position.TOP_CENTER);
                postalCodeField.focus();
                return;
            }
            
            String finalApartmentNumber = (apartmentNumber != null && !apartmentNumber.trim().isEmpty()) ? apartmentNumber.trim() : null;
            
            System.out.println("CheckoutView: Saving address - city=" + city + ", street=" + street + ", postalCode=" + postalCode + ", apartmentNumber=" + finalApartmentNumber);
            
            getElement().executeJs(
                "const userId = localStorage.getItem('eatgo-userId'); " +
                "console.log('CheckoutView: userId from localStorage:', userId); " +
                "if (userId) { " +
                "  $0.$server.saveNewAddress($1, $2, $3, $4 || null, userId); " +
                "} else { " +
                "  console.error('CheckoutView: No userId in localStorage'); " +
                "  $0.$server.onAddressError('Użytkownik niezalogowany'); " +
                "}",
                getElement(), 
                city.trim(), 
                street.trim(), 
                postalCode.trim(),
                finalApartmentNumber != null ? finalApartmentNumber : ""
            );
        });
        
        Button cancelBtn = new Button("Anuluj");
        cancelBtn.addClickListener(e -> addressDialog.close());
        
        addressDialog.getFooter().add(cancelBtn, saveBtn);
        addressDialog.add(form);
        addressDialog.open();
    }
    
    @com.vaadin.flow.component.ClientCallable
    public void saveNewAddress(String city, String street, String postalCode, String apartmentNumber, String userIdStr) {
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                try {
                    System.out.println("CheckoutView.saveNewAddress: city=" + city + ", street=" + street + ", postalCode=" + postalCode + ", apartmentNumber=" + apartmentNumber + ", userId=" + userIdStr);
                    
                    if (city == null || city.isEmpty() || street == null || street.isEmpty() || postalCode == null || postalCode.isEmpty()) {
                        Notification.show("Wszystkie wymagane pola muszą być wypełnione.", 3000, Notification.Position.TOP_CENTER);
                        return;
                    }
                    
                    Long userId = Long.parseLong(userIdStr);
                    String finalApartmentNumber = (apartmentNumber != null && !apartmentNumber.trim().isEmpty()) ? apartmentNumber.trim() : null;
                    AddressCreateDto newAddress = new AddressCreateDto(city.trim(), street.trim(), postalCode.trim(), finalApartmentNumber);
                    AddressDto savedAddress = addressService.addAddress(userId, newAddress);
                    
                    System.out.println("CheckoutView.saveNewAddress: Address saved successfully, ID: " + savedAddress.id());
                    
                    Notification.show("Adres dodany pomyślnie!", 3000, Notification.Position.TOP_CENTER);
                    
                    if (addressDialog != null) {
                        System.out.println("CheckoutView.saveNewAddress: Closing dialog");
                        addressDialog.close();
                        addressDialog = null;
                    }
                    
                    currentAddresses.add(0, savedAddress);
                    addressComboBox.setItems(currentAddresses);
                    addressComboBox.setValue(savedAddress);
                } catch (NumberFormatException ex) {
                    System.err.println("CheckoutView: Invalid userId format: " + userIdStr);
                    Notification.show("Błąd: Nieprawidłowy identyfikator użytkownika.", 3000, Notification.Position.TOP_CENTER);
                } catch (Exception ex) {
                    System.err.println("CheckoutView: Error adding address: " + ex.getMessage());
                    ex.printStackTrace();
                    Notification.show("Błąd podczas dodawania adresu: " + ex.getMessage(), 3000, Notification.Position.TOP_CENTER);
                }
            });
        });
    }
    
    @com.vaadin.flow.component.ClientCallable
    public void selectAddress(String addressIdStr) {
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                try {
                    Long addressId = Long.parseLong(addressIdStr);
                    System.out.println("CheckoutView.selectAddress: Selecting address ID: " + addressId);
                    
                    AddressDto found = currentAddresses.stream()
                        .filter(a -> a.id().equals(addressId))
                        .findFirst()
                        .orElse(null);
                    if (found != null) {
                        addressComboBox.setValue(found);
                        System.out.println("CheckoutView.selectAddress: Address selected: " + found.id());
                    } else {
                        System.out.println("CheckoutView.selectAddress: Address not found in list: " + addressId);
                        loadUserAddresses();
                    }
                } catch (Exception e) {
                    System.err.println("CheckoutView.selectAddress: Error selecting address: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        });
    }
    
    @com.vaadin.flow.component.ClientCallable
    public void onAddressError(String errorMessage) {
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                Notification.show("Błąd: " + errorMessage, 3000, Notification.Position.TOP_CENTER);
            });
        });
    }
    
    private void placeOrder() {
        if (addressComboBox.getValue() == null) {
            Notification.show("Wybierz adres dostawy", 3000, Notification.Position.TOP_CENTER);
            return;
        }
        
        if (paymentMethodGroup.getValue() == null) {
            Notification.show("Wybierz metodę płatności", 3000, Notification.Position.TOP_CENTER);
            return;
        }
        
        if (cartItems.isEmpty()) {
            Notification.show("Koszyk jest pusty", 3000, Notification.Position.TOP_CENTER);
            return;
        }
        
        AddressDto selectedAddress = addressComboBox.getValue();
        
        String itemsJson = cartItems.stream()
            .map(item -> String.format("{\"dishId\":%d,\"quantity\":%d}", item.dishId(), item.quantity()))
            .collect(Collectors.joining(",", "[", "]"));
        
        System.out.println("CheckoutView: Placing order - restaurantId: " + restaurantId + 
                          ", addressId: " + selectedAddress.id() + 
                          ", items: " + itemsJson);
        
        getElement().executeJs(
            "const token = localStorage.getItem('eatgo-token'); " +
            "if (!token) { " +
            "  $0.$server.onOrderError('Musisz być zalogowany, aby złożyć zamówienie'); " +
            "  return; " +
            "} " +
            "" +
            "const orderData = { " +
            "  restaurantId: Number($1), " +
            "  addressId: Number($2), " +
            "  items: JSON.parse($3) " +
            "}; " +
            "" +
            "console.log('CheckoutView: Sending order:', orderData); " +
            "" +
            "fetch('/api/orders', { " +
            "  method: 'POST', " +
            "  headers: { " +
            "    'Content-Type': 'application/json', " +
            "    'Authorization': 'Bearer ' + token " +
            "  }, " +
            "  body: JSON.stringify(orderData) " +
            "}) " +
            ".then(response => { " +
            "  console.log('CheckoutView: Order response status:', response.status); " +
            "  if (!response.ok) { " +
            "    return response.text().then(text => { " +
            "      try { " +
            "        const errorJson = JSON.parse(text); " +
            "        throw new Error(errorJson.message || errorJson.error || text); " +
            "      } catch (e) { " +
            "        throw new Error(text || 'Błąd podczas składania zamówienia'); " +
            "      } " +
            "    }); " +
            "  } " +
            "  return response.json(); " +
            "}) " +
            ".then(data => { " +
            "  console.log('CheckoutView: Order created successfully:', data); " +
            "  $0.$server.onOrderSuccess(JSON.stringify(data)); " +
            "}) " +
            ".catch(error => { " +
            "  console.error('CheckoutView: Order error:', error); " +
            "  $0.$server.onOrderError(error.message || 'Błąd podczas składania zamówienia'); " +
            "});",
            getElement(), String.valueOf(restaurantId), String.valueOf(selectedAddress.id()), itemsJson
        );
    }
    
    @com.vaadin.flow.component.ClientCallable
    public void onOrderSuccess(String orderJson) {
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                try {
                    System.out.println("CheckoutView.onOrderSuccess: Order JSON: " + orderJson);
                    
                    getElement().executeJs(
                        "const carts = JSON.parse(localStorage.getItem('eatgo-carts') || '{}'); " +
                        "delete carts[$0]; " +
                        "localStorage.setItem('eatgo-carts', JSON.stringify(carts)); " +
                        "window.dispatchEvent(new Event('eatgo-cart-changed'));",
                        String.valueOf(restaurantId)
                    );
                    
                    getElement().executeJs(
                        "const token = localStorage.getItem('eatgo-token'); " +
                        "const orderData = JSON.parse($0); " +
                        "fetch('/api/orders/' + orderData.id, { " +
                        "  headers: { 'Authorization': 'Bearer ' + token } " +
                        "}) " +
                        ".then(r => r.json()) " +
                        ".then(details => { " +
                        "  $1.$server.showOrderConfirmation(JSON.stringify(details)); " +
                        "}) " +
                        ".catch(e => { " +
                        "  console.error('Error fetching order details:', e); " +
                        "  $1.$server.showOrderConfirmation($0); " +
                        "});",
                        orderJson, getElement()
                    );
                } catch (Exception e) {
                    System.err.println("CheckoutView: Error processing order success: " + e.getMessage());
                    e.printStackTrace();
                    Notification.show("Zamówienie złożone, ale wystąpił błąd przy wyświetlaniu szczegółów", 3000, Notification.Position.TOP_CENTER);
                    getUI().ifPresent(u -> u.navigate("orders"));
                }
            });
        });
    }
    
    @com.vaadin.flow.component.ClientCallable
    public void showOrderConfirmation(String orderDetailsJson) {
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                    mapper.disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
                    
                    pollub.eatgo.dto.order.OrderDetailsDto orderDetails = mapper.readValue(
                        orderDetailsJson, 
                        pollub.eatgo.dto.order.OrderDetailsDto.class
                    );
                    
                    showOrderConfirmationDialog(orderDetails);
                } catch (Exception e) {
                    System.err.println("CheckoutView: Error parsing order details: " + e.getMessage());
                    e.printStackTrace();
                    Notification.show("Zamówienie złożone pomyślnie!", 3000, Notification.Position.TOP_CENTER);
                    getUI().ifPresent(u -> u.navigate("orders"));
                }
            });
        });
    }
    
    private void showOrderConfirmationDialog(pollub.eatgo.dto.order.OrderDetailsDto orderDetails) {
        Dialog confirmationDialog = new Dialog();
        confirmationDialog.setHeaderTitle("Zamówienie złożone pomyślnie!");
        confirmationDialog.setModal(true);
        confirmationDialog.setCloseOnEsc(true);
        confirmationDialog.setCloseOnOutsideClick(false);
        
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(true);
        
        Div statusDiv = new Div();
        statusDiv.addClassName("order-confirmation-status");
        Span statusLabel = new Span("Status: ");
        statusLabel.getStyle().set("font-weight", "bold");
        Span statusValue = new Span(getStatusLabel(orderDetails.status()));
        statusValue.getStyle().set("color", getStatusColor(orderDetails.status()));
        statusDiv.add(statusLabel, statusValue);
        
        Div orderIdDiv = new Div();
        orderIdDiv.addClassName("order-confirmation-id");
        Span orderIdLabel = new Span("Numer zamówienia: ");
        orderIdLabel.getStyle().set("font-weight", "bold");
        Span orderIdValue = new Span("#" + orderDetails.id());
        orderIdDiv.add(orderIdLabel, orderIdValue);
        
        Div restaurantDiv = new Div();
        restaurantDiv.addClassName("order-confirmation-restaurant");
        Span restaurantLabel = new Span("Restauracja: ");
        restaurantLabel.getStyle().set("font-weight", "bold");
        Span restaurantValue = new Span(orderDetails.restaurant().name());
        restaurantDiv.add(restaurantLabel, restaurantValue);
        
        Div addressDiv = new Div();
        addressDiv.addClassName("order-confirmation-address");
        Span addressLabel = new Span("Adres dostawy: ");
        addressLabel.getStyle().set("font-weight", "bold");
        String fullAddress = orderDetails.deliveryAddress().street();
        if (orderDetails.deliveryAddress().apartmentNumber() != null && !orderDetails.deliveryAddress().apartmentNumber().isEmpty()) {
            fullAddress += "/" + orderDetails.deliveryAddress().apartmentNumber();
        }
        fullAddress += ", " + orderDetails.deliveryAddress().postalCode() + " " + orderDetails.deliveryAddress().city();
        Span addressValue = new Span(fullAddress);
        addressDiv.add(addressLabel, addressValue);
        
        Div totalDiv = new Div();
        totalDiv.addClassName("order-confirmation-total");
        totalDiv.getStyle().set("border-top", "1px solid var(--lumo-contrast-10pct)");
        totalDiv.getStyle().set("padding-top", "1rem");
        totalDiv.getStyle().set("margin-top", "1rem");
        Span totalLabel = new Span("Wartość zamówienia: ");
        totalLabel.getStyle().set("font-weight", "bold");
        totalLabel.getStyle().set("font-size", "1.1rem");
        Span totalValue = new Span(String.format("%.2f zł", orderDetails.totalPrice().doubleValue()));
        totalValue.getStyle().set("font-weight", "bold");
        totalValue.getStyle().set("font-size", "1.1rem");
        totalValue.getStyle().set("color", "var(--lumo-primary-color)");
        totalDiv.add(totalLabel, totalValue);
        
        content.add(statusDiv, orderIdDiv, restaurantDiv, addressDiv, totalDiv);
        
        Button okBtn = new Button("OK", VaadinIcon.CHECK.create());
        okBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        okBtn.addClickListener(e -> {
            confirmationDialog.close();
            getUI().ifPresent(u -> u.navigate("orders"));
        });
        
        confirmationDialog.getFooter().add(okBtn);
        confirmationDialog.add(content);
        confirmationDialog.open();
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
            case "COOKING", "READY", "IN_DELIVERY" -> "var(--lumo-warning-color)";
            case "DELIVERED" -> "var(--lumo-success-color)";
            case "CANCELLED" -> "var(--lumo-error-color)";
            default -> "var(--lumo-secondary-text-color)";
        };
    }
    
    @com.vaadin.flow.component.ClientCallable
    public void onOrderError(String errorMessage) {
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                System.err.println("CheckoutView: Order error: " + errorMessage);
                Notification.show("Błąd: " + errorMessage, 5000, Notification.Position.TOP_CENTER);
            });
        });
    }
}

