package pollub.eatgo.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import pollub.eatgo.dto.address.AddressCreateDto;
import pollub.eatgo.dto.address.AddressDto;
import pollub.eatgo.service.AddressService;
import pollub.eatgo.service.AuthenticationService;
import pollub.eatgo.service.OrderNotificationService;
import pollub.eatgo.service.TokenValidationService;
import pollub.eatgo.views.components.HeaderComponent;

import java.util.List;

@Route("addresses")
@PageTitle("EatGo - Moje adresy")
public class AddressesView extends VerticalLayout {
    
    private final AddressService addressService;
    private final AuthenticationService authService;
    private final TokenValidationService tokenValidationService;
    private VerticalLayout addressesContainer;
    private Dialog addressDialog;
    private AddressDto editingAddress;
    
    public AddressesView(AddressService addressService,
                         AuthenticationService authService,
                         TokenValidationService tokenValidationService,
                         OrderNotificationService orderNotificationService) {
        this.addressService = addressService;
        this.authService = authService;
        this.tokenValidationService = tokenValidationService;
        
        setSizeFull();
        setSpacing(false);
        setPadding(false);
        
        HeaderComponent headerComponent = new HeaderComponent(authService, tokenValidationService, orderNotificationService);
        add(headerComponent);
        
        Div content = new Div();
        content.addClassName("addresses-content");
        content.getStyle().set("max-width", "1200px");
        content.getStyle().set("margin", "0 auto");
        content.getStyle().set("padding", "2rem");
        
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        
        H2 title = new H2("Moje adresy");
        title.addClassName("addresses-title");
        
        Button addButton = new Button("Dodaj nowy adres", VaadinIcon.PLUS.create());
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(e -> showAddAddressDialog());
        
        header.add(title, addButton);
        
        addressesContainer = new VerticalLayout();
        addressesContainer.setSpacing(true);
        addressesContainer.setPadding(false);
        addressesContainer.setWidthFull();
        
        content.add(header, addressesContainer);
        add(content);
        
        loadAddresses();
    }
    
    private void loadAddresses() {
        getElement().executeJs(
            "const userId = localStorage.getItem('eatgo-userId'); " +
            "if (userId) { " +
            "  $0.$server.loadAddresses(userId); " +
            "} else { " +
            "  $0.$server.onError('Użytkownik niezalogowany'); " +
            "}",
            getElement()
        );
    }
    
    @com.vaadin.flow.component.ClientCallable
    public void loadAddresses(String userIdStr) {
        System.out.println("AddressesView.loadAddresses called with userId: " + userIdStr);
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                try {
                    Long userId = Long.parseLong(userIdStr);
                    System.out.println("AddressesView.loadAddresses: Loading addresses for userId: " + userId);
                    List<AddressDto> addresses = addressService.listAddresses(userId);
                    System.out.println("AddressesView.loadAddresses: Loaded " + addresses.size() + " addresses");
                    displayAddresses(addresses);
                } catch (Exception e) {
                    System.err.println("AddressesView: Error loading addresses: " + e.getMessage());
                    e.printStackTrace();
                    Notification.show("Błąd podczas ładowania adresów: " + e.getMessage(), 3000, Notification.Position.TOP_CENTER);
                }
            });
        });
    }
    
    private void displayAddresses(List<AddressDto> addresses) {
        addressesContainer.removeAll();
        
        if (addresses.isEmpty()) {
            Div emptyMessage = new Div();
            emptyMessage.addClassName("empty-addresses-message");
            emptyMessage.setText("Nie masz jeszcze żadnych zapisanych adresów.");
            emptyMessage.getStyle().set("text-align", "center");
            emptyMessage.getStyle().set("padding", "3rem");
            emptyMessage.getStyle().set("color", "var(--lumo-secondary-text-color)");
            addressesContainer.add(emptyMessage);
            return;
        }
        
        for (AddressDto address : addresses) {
            Div addressCard = createAddressCard(address);
            addressesContainer.add(addressCard);
        }
    }
    
    private Div createAddressCard(AddressDto address) {
        Div card = new Div();
        card.addClassName("address-card");
        card.getStyle().set("padding", "1.5rem");
        card.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        card.getStyle().set("border-radius", "8px");
        card.getStyle().set("background", "var(--lumo-base-color)");
        
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(false);
        content.setPadding(false);
        
        H3 streetHeader = new H3(address.street());
        streetHeader.getStyle().set("margin", "0");
        streetHeader.getStyle().set("font-size", "1.25rem");
        
        Div addressDetails = new Div();
        addressDetails.getStyle().set("margin-top", "0.5rem");
        
        String fullAddress = address.street();
        if (address.apartmentNumber() != null && !address.apartmentNumber().isEmpty()) {
            fullAddress += "/" + address.apartmentNumber();
        }
        fullAddress += ", " + address.postalCode() + " " + address.city();
        
        addressDetails.setText(fullAddress);
        addressDetails.getStyle().set("color", "var(--lumo-secondary-text-color)");
        
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(true);
        actions.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.END);
        
        Button editButton = new Button("Edytuj", VaadinIcon.EDIT.create());
        editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        editButton.addClickListener(e -> showEditAddressDialog(address));
        
        Button deleteButton = new Button("Usuń", VaadinIcon.TRASH.create());
        deleteButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        deleteButton.addClickListener(e -> showDeleteConfirmDialog(address));
        
        actions.add(editButton, deleteButton);
        
        content.add(streetHeader, addressDetails, actions);
        card.add(content);
        
        return card;
    }
    
    private void showAddAddressDialog() {
        editingAddress = null;
        addressDialog = new Dialog();
        addressDialog.setHeaderTitle("Dodaj nowy adres");
        addressDialog.setModal(true);
        addressDialog.setCloseOnEsc(true);
        addressDialog.setCloseOnOutsideClick(false);
        
        createAddressForm(null);
    }
    
    private void showEditAddressDialog(AddressDto address) {
        editingAddress = address;
        addressDialog = new Dialog();
        addressDialog.setHeaderTitle("Edytuj adres");
        addressDialog.setModal(true);
        addressDialog.setCloseOnEsc(true);
        addressDialog.setCloseOnOutsideClick(false);
        
        createAddressForm(address);
    }
    
    private void createAddressForm(AddressDto address) {
        TextField streetField = new TextField("Ulica");
        streetField.setWidthFull();
        if (address != null) {
            streetField.setValue(address.street());
        }
        
        com.vaadin.flow.component.combobox.ComboBox<String> cityField = new com.vaadin.flow.component.combobox.ComboBox<>("Miasto");
        cityField.setWidthFull();
        cityField.setItems("Warszawa", "Lublin", "Rzeszów");
        if (address != null) {
            cityField.setValue(address.city());
        }
        
        TextField postalCodeField = new TextField("Kod pocztowy");
        postalCodeField.setWidthFull();
        if (address != null) {
            postalCodeField.setValue(address.postalCode());
        }
        
        TextField apartmentNumberField = new TextField("Numer mieszkania/piętro (opcjonalnie)");
        apartmentNumberField.setWidthFull();
        apartmentNumberField.setPlaceholder("np. 5/12 lub 3. piętro");
        if (address != null && address.apartmentNumber() != null) {
            apartmentNumberField.setValue(address.apartmentNumber());
        }
        
        TextArea notesField = new TextArea("Uwagi (opcjonalnie)");
        notesField.setWidthFull();
        notesField.setVisible(false); // Ukryj na razie
        
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
            
            // Walidacja - numer mieszkania może być pusty
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
            
            // Numer mieszkania może być pusty - to jest OK
            String finalApartmentNumber = (apartmentNumber != null && !apartmentNumber.trim().isEmpty()) ? apartmentNumber.trim() : "";
            
            System.out.println("AddressesView: Saving address - city=" + city + ", street=" + street + ", postalCode=" + postalCode + ", apartmentNumber=" + finalApartmentNumber);
            
            // Wyłącz przycisk, aby zapobiec wielokrotnym kliknięciom
            saveBtn.setEnabled(false);
            saveBtn.setText("Zapisywanie...");
            
            // Pobierz userId z localStorage i wywołaj metodę
            getElement().executeJs(
                "return localStorage.getItem('eatgo-userId');"
            ).then(String.class, userIdStr -> {
                if (userIdStr != null && !userIdStr.isEmpty()) {
                    System.out.println("AddressesView: Got userId from localStorage: " + userIdStr);
                    // Wywołaj metodę bezpośrednio z Java
                    saveAddressDirect(city.trim(), street.trim(), postalCode.trim(), 
                        finalApartmentNumber != null ? finalApartmentNumber : "", userIdStr);
                } else {
                    System.err.println("AddressesView: No userId in localStorage");
                    Notification.show("Użytkownik niezalogowany", 3000, Notification.Position.TOP_CENTER);
                    saveBtn.setEnabled(true);
                    saveBtn.setText("Zapisz");
                }
            });
        });
        
        Button cancelBtn = new Button("Anuluj");
        cancelBtn.addClickListener(e -> addressDialog.close());
        
        addressDialog.getFooter().add(cancelBtn, saveBtn);
        addressDialog.add(form);
        addressDialog.open();
    }
    
    // Metoda wywoływana bezpośrednio z Java (nie przez JavaScript)
    private void saveAddressDirect(String city, String street, String postalCode, String apartmentNumber, String userIdStr) {
        System.out.println("=== AddressesView.saveAddressDirect CALLED ===");
        System.out.println("Parameters: city=" + city + ", street=" + street + ", postalCode=" + postalCode + ", apartmentNumber=" + apartmentNumber + ", userId=" + userIdStr);
        
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                try {
                    System.out.println("AddressesView.saveAddressDirect: Inside UI.access");
                    
                    if (city == null || city.isEmpty() || street == null || street.isEmpty() || postalCode == null || postalCode.isEmpty()) {
                        System.err.println("AddressesView.saveAddressDirect: Validation failed - missing required fields");
                        Notification.show("Wszystkie wymagane pola muszą być wypełnione.", 3000, Notification.Position.TOP_CENTER);
                        return;
                    }
                    
                    System.out.println("AddressesView.saveAddressDirect: Parsing userId: " + userIdStr);
                    Long userId = Long.parseLong(userIdStr);
                    System.out.println("AddressesView.saveAddressDirect: Parsed userId: " + userId);
                    
                    // Jeśli apartmentNumber jest pustym stringiem, ustaw na null
                    String finalApartmentNumber = (apartmentNumber != null && !apartmentNumber.trim().isEmpty()) ? apartmentNumber.trim() : null;
                    System.out.println("AddressesView.saveAddressDirect: Final apartmentNumber: " + finalApartmentNumber);
                    
                    AddressCreateDto addressDto = new AddressCreateDto(city.trim(), street.trim(), postalCode.trim(), finalApartmentNumber);
                    System.out.println("AddressesView.saveAddressDirect: Created DTO");
                    
                    if (editingAddress != null) {
                        // Edycja istniejącego adresu
                        System.out.println("AddressesView: Updating address ID: " + editingAddress.id());
                        AddressDto updated = addressService.updateAddress(userId, editingAddress.id(), addressDto);
                        System.out.println("AddressesView: Address updated successfully, ID: " + updated.id());
                        Notification.show("Adres zaktualizowany pomyślnie!", 3000, Notification.Position.TOP_CENTER);
                    } else {
                        // Dodawanie nowego adresu
                        System.out.println("AddressesView: Adding new address for userId: " + userId);
                        AddressDto saved = addressService.addAddress(userId, addressDto);
                        System.out.println("AddressesView: Address added successfully, ID: " + saved.id());
                        Notification.show("Adres dodany pomyślnie!", 3000, Notification.Position.TOP_CENTER);
                    }
                    
                    System.out.println("AddressesView.saveAddressDirect: Closing dialog");
                    if (addressDialog != null) {
                        addressDialog.close();
                        addressDialog = null;
                    }
                    
                    editingAddress = null;
                    System.out.println("AddressesView.saveAddressDirect: Reloading addresses");
                    
                    // Odśwież listę adresów bezpośrednio (userId jest już zdefiniowane wcześniej)
                    List<AddressDto> addresses = addressService.listAddresses(userId);
                    System.out.println("AddressesView.saveAddressDirect: Reloaded " + addresses.size() + " addresses");
                    displayAddresses(addresses);
                    
                    System.out.println("=== AddressesView.saveAddressDirect COMPLETED ===");
                } catch (NumberFormatException ex) {
                    System.err.println("AddressesView: Invalid userId format: " + userIdStr);
                    ex.printStackTrace();
                    Notification.show("Błąd: Nieprawidłowy identyfikator użytkownika.", 3000, Notification.Position.TOP_CENTER);
                } catch (Exception ex) {
                    System.err.println("AddressesView: Error saving address: " + ex.getMessage());
                    ex.printStackTrace();
                    Notification.show("Błąd podczas zapisywania adresu: " + ex.getMessage(), 3000, Notification.Position.TOP_CENTER);
                }
            });
        });
    }
    
    private void showDeleteConfirmDialog(AddressDto address) {
        com.vaadin.flow.component.dialog.Dialog confirmDialog = new com.vaadin.flow.component.dialog.Dialog();
        confirmDialog.setHeaderTitle("Potwierdź usunięcie");
        
        Div message = new Div();
        message.setText("Czy na pewno chcesz usunąć ten adres?");
        message.getStyle().set("padding", "1rem");
        
        Button confirmBtn = new Button("Usuń", VaadinIcon.TRASH.create());
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        confirmBtn.addClickListener(e -> {
            deleteAddress(address);
            confirmDialog.close();
        });
        
        Button cancelBtn = new Button("Anuluj");
        cancelBtn.addClickListener(e -> confirmDialog.close());
        
        confirmDialog.getFooter().add(cancelBtn, confirmBtn);
        confirmDialog.add(message);
        confirmDialog.open();
    }
    
    private void deleteAddress(AddressDto address) {
        getElement().executeJs(
            "const userId = localStorage.getItem('eatgo-userId'); " +
            "if (userId) { " +
            "  $0.$server.deleteAddress($1, userId); " +
            "} else { " +
            "  $0.$server.onError('Użytkownik niezalogowany'); " +
            "}",
            getElement(),
            String.valueOf(address.id())
        );
    }
    
    @com.vaadin.flow.component.ClientCallable
    public void deleteAddress(String addressIdStr, String userIdStr) {
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                try {
                    Long userId = Long.parseLong(userIdStr);
                    Long addressId = Long.parseLong(addressIdStr);
                    addressService.deleteAddress(userId, addressId);
                    Notification.show("Adres usunięty pomyślnie!", 3000, Notification.Position.TOP_CENTER);
                    loadAddresses();
                } catch (Exception ex) {
                    System.err.println("AddressesView: Error deleting address: " + ex.getMessage());
                    ex.printStackTrace();
                    Notification.show("Błąd podczas usuwania adresu: " + ex.getMessage(), 3000, Notification.Position.TOP_CENTER);
                }
            });
        });
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

