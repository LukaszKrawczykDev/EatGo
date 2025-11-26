package pollub.eatgo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import pollub.eatgo.dto.order.OrderCreateRequestDto;
import pollub.eatgo.dto.order.OrderDetailsDto;
import pollub.eatgo.dto.order.OrderDto;
import pollub.eatgo.dto.order.OrderItemRequestDto;
import pollub.eatgo.model.*;
import pollub.eatgo.repository.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(locations = "classpath:application-test.properties")
@Import(OrderService.class)
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private DishRepository dishRepository;

    private User client;
    private Restaurant restaurant;
    private Address address;
    private Dish dish1;
    private Dish dish2;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        addressRepository.deleteAll();
        dishRepository.deleteAll();
        restaurantRepository.deleteAll();
        userRepository.deleteAll();

        client = User.builder()
                .email("client@example.com")
                .password("secret")
                .fullName("Client User")
                .role(User.Role.CLIENT)
                .build();
        client = userRepository.save(client);

        restaurant = Restaurant.builder()
                .name("Test Restaurant")
                .address("Testowa 1")
                .deliveryPrice(5.0)
                .build();
        restaurant = restaurantRepository.save(restaurant);

        address = Address.builder()
                .city("Warszawa")
                .street("Testowa 1")
                .postalCode("00-001")
                .apartmentNumber("5")
                .user(client)
                .build();
        address = addressRepository.save(address);

        dish1 = Dish.builder()
                .name("Pizza Margherita")
                .price(20.0)
                .available(true)
                .restaurant(restaurant)
                .build();
        dish2 = Dish.builder()
                .name("Burger Classic")
                .price(25.0)
                .available(true)
                .restaurant(restaurant)
                .build();
        dish1 = dishRepository.save(dish1);
        dish2 = dishRepository.save(dish2);
    }

    private OrderCreateRequestDto createRequest() {
        return new OrderCreateRequestDto(
                restaurant.getId(),
                address.getId(),
                List.of(
                        new OrderItemRequestDto(dish1.getId(), 2),
                        new OrderItemRequestDto(dish2.getId(), 1)
                )
        );
    }

    @Test
    void createOrder_Integration() {
        // When
        OrderDto dto = orderService.createOrder(client.getId(), createRequest());

        // Then
        assertNotNull(dto);
        assertNotNull(dto.id());
        assertEquals("PLACED", dto.status());
        assertEquals(70.0, dto.totalPrice(), 0.001); // 2*20 + 1*25 + 5

        Order saved = orderRepository.findById(dto.id()).orElseThrow();
        assertEquals(client.getId(), saved.getUser().getId());
        assertEquals(restaurant.getId(), saved.getRestaurant().getId());
        assertEquals(address.getId(), saved.getAddress().getId());
        // Powinny być 2 rekordy OrderItem (po jednym na danie), a ilość jest w polu quantity
        assertEquals(2, saved.getItems().size());
        int totalQuantity = saved.getItems().stream().mapToInt(OrderItem::getQuantity).sum();
        assertEquals(3, totalQuantity);
    }

    @Test
    void listUserOrders_Integration() {
        // Given
        orderService.createOrder(client.getId(), createRequest());
        orderService.createOrder(client.getId(), createRequest());

        // When
        List<OrderDto> orders = orderService.listUserOrders(client.getId());

        // Then
        assertEquals(2, orders.size());
        assertTrue(orders.get(0).createdAt().isAfter(orders.get(1).createdAt())
                || orders.get(0).createdAt().isEqual(orders.get(1).createdAt()));
    }

    @Test
    void getOrderDetails_Integration() {
        // Given
        OrderDto dto = orderService.createOrder(client.getId(), createRequest());

        // When
        OrderDetailsDto details = orderService.getOrderDetails(client.getId(), dto.id());

        // Then
        assertNotNull(details);
        assertEquals(dto.id(), details.id());
        assertEquals("PLACED", details.status());
        assertEquals(2, details.items().size());
        assertNotNull(details.restaurant());
        assertNotNull(details.deliveryAddress());
        assertEquals(restaurant.getName(), details.restaurant().name());
    }
}


