package pollub.eatgo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pollub.eatgo.dto.order.OrderCreateRequestDto;
import pollub.eatgo.dto.order.OrderDto;
import pollub.eatgo.dto.order.OrderItemRequestDto;
import pollub.eatgo.model.*;
import pollub.eatgo.repository.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceUnitTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RestaurantRepository restaurantRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private DishRepository dishRepository;

    @InjectMocks
    private OrderService orderService;

    private User user;
    private Restaurant restaurant;
    private Address address;
    private Dish dish1;
    private Dish dish2;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("client@example.com")
                .fullName("Client User")
                .role(User.Role.CLIENT)
                .build();

        restaurant = Restaurant.builder()
                .id(10L)
                .name("Test Restaurant")
                .address("Testowa 1")
                .deliveryPrice(5.0)
                .build();

        address = Address.builder()
                .id(100L)
                .city("Warszawa")
                .street("Testowa 1")
                .postalCode("00-001")
                .apartmentNumber("5")
                .user(user)
                .build();

        dish1 = Dish.builder()
                .id(1000L)
                .name("Pizza Margherita")
                .price(20.0)
                .available(true)
                .restaurant(restaurant)
                .build();

        dish2 = Dish.builder()
                .id(1001L)
                .name("Burger Classic")
                .price(25.0)
                .available(true)
                .restaurant(restaurant)
                .build();
    }

    private OrderCreateRequestDto createValidRequest() {
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
    void createOrder_ShouldCalculateTotalPriceAndDelivery() {
        // Given
        OrderCreateRequestDto req = createValidRequest();

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(restaurantRepository.findById(restaurant.getId())).thenReturn(Optional.of(restaurant));
        when(addressRepository.findByIdAndUserId(address.getId(), user.getId())).thenReturn(Optional.of(address));
        when(dishRepository.findById(dish1.getId())).thenReturn(Optional.of(dish1));
        when(dishRepository.findById(dish2.getId())).thenReturn(Optional.of(dish2));

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(999L);
            return o;
        });

        // When
        OrderDto result = orderService.createOrder(user.getId(), req);

        // Then
        assertNotNull(result);
        assertEquals(999L, result.id());
        // items: 2 * 20 + 1 * 25 = 65; delivery 5 => total 70
        assertEquals(70.0, result.totalPrice(), 0.001);
        assertEquals(5.0, result.deliveryPrice(), 0.001);
        assertEquals("PLACED", result.status());

        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void createOrder_ShouldThrowWhenDishFromAnotherRestaurant() {
        // Given
        Restaurant otherRestaurant = Restaurant.builder()
                .id(20L)
                .name("Other")
                .deliveryPrice(7.0)
                .build();

        Dish foreignDish = Dish.builder()
                .id(2000L)
                .name("Foreign Dish")
                .price(15.0)
                .available(true)
                .restaurant(otherRestaurant)
                .build();

        OrderCreateRequestDto req = new OrderCreateRequestDto(
                restaurant.getId(),
                address.getId(),
                List.of(new OrderItemRequestDto(foreignDish.getId(), 1))
        );

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(restaurantRepository.findById(restaurant.getId())).thenReturn(Optional.of(restaurant));
        when(addressRepository.findByIdAndUserId(address.getId(), user.getId())).thenReturn(Optional.of(address));
        when(dishRepository.findById(foreignDish.getId())).thenReturn(Optional.of(foreignDish));

        // When & Then
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> orderService.createOrder(user.getId(), req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("does not belong to the restaurant"));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrder_ShouldThrowWhenDishNotAvailable() {
        // Given
        dish1.setAvailable(false);
        OrderCreateRequestDto req = new OrderCreateRequestDto(
                restaurant.getId(),
                address.getId(),
                List.of(new OrderItemRequestDto(dish1.getId(), 1))
        );

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(restaurantRepository.findById(restaurant.getId())).thenReturn(Optional.of(restaurant));
        when(addressRepository.findByIdAndUserId(address.getId(), user.getId())).thenReturn(Optional.of(address));
        when(dishRepository.findById(dish1.getId())).thenReturn(Optional.of(dish1));

        // When & Then
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> orderService.createOrder(user.getId(), req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("is not available"));
        verify(orderRepository, never()).save(any(Order.class));
    }
}


