package pollub.eatgo.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.password.PasswordEncoder;
import pollub.eatgo.dto.courier.CourierCreateDto;
import pollub.eatgo.dto.courier.CourierDto;
import pollub.eatgo.dto.courier.CourierUpdateDto;
import pollub.eatgo.dto.dish.DishCreateDto;
import pollub.eatgo.dto.dish.DishDto;
import pollub.eatgo.dto.dish.DishUpdateDto;
import pollub.eatgo.dto.order.OrderDto;
import pollub.eatgo.dto.order.OrderItemDto;
import pollub.eatgo.dto.restaurant.RestaurantDto;
import pollub.eatgo.dto.restaurant.RestaurantSummaryDto;
import pollub.eatgo.dto.restaurant.RestaurantUpdateDto;
import pollub.eatgo.model.*;
import pollub.eatgo.model.OrderStatus;
import pollub.eatgo.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Połączony serwis dla funkcjonalności "restaurant" (admin) oraz podstawowych metod klienta.
 * Usuń poprzednie duplikaty klas (zwłaszcza z pakietu service.client) aby uniknąć konfliktów.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RestaurantService {

    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final OrderRepository orderRepository;
    private final DishRepository dishRepository;
	private final PasswordEncoder passwordEncoder;
    private final OrderNotificationService orderNotificationService;

    // ----------------- ADMIN / RESTAURANT (protected endpoints) -----------------

    public List<OrderDto> listOrders(String adminEmail) {
        Restaurant restaurant = resolveRestaurantForAdmin(adminEmail);
        List<Order> orders = orderRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurant.getId());
        return orders.stream().map(this::toOrderDto).collect(Collectors.toList());
    }

    public OrderDto updateOrderStatus(String adminEmail, Long orderId, String status) {
        Restaurant restaurant = resolveRestaurantForAdmin(adminEmail);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!order.getRestaurant().getId().equals(restaurant.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Order does not belong to your restaurant");
        }
        OrderStatus previousStatus = order.getStatus();
        OrderStatus targetStatus = parseOrderStatus(status);
        validateAdminTransition(order.getStatus(), targetStatus);
        if (targetStatus == OrderStatus.IN_DELIVERY && order.getCourier() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assign courier before marking order IN_DELIVERY");
        }
        order.setStatus(targetStatus);
        order = orderRepository.save(order);
        orderNotificationService.addStatusChangeNotification(order, previousStatus, targetStatus);
        return toOrderDto(order);
    }

    public OrderDto assignCourier(String adminEmail, Long orderId, Long courierId) {
        Restaurant restaurant = resolveRestaurantForAdmin(adminEmail);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!order.getRestaurant().getId().equals(restaurant.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Order does not belong to your restaurant");
        }
        if (order.getStatus() != OrderStatus.READY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order must be READY before assigning courier");
        }
        User courier = userRepository.findByIdAndRestaurantId(courierId, restaurant.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Courier not found for your restaurant"));
        OrderStatus previousStatus = order.getStatus();
        order.setCourier(courier);
        order.setStatus(OrderStatus.IN_DELIVERY);
        order = orderRepository.save(order);
        // Powiadom klienta, że zamówienie jest w drodze
        orderNotificationService.addStatusChangeNotification(order, previousStatus, OrderStatus.IN_DELIVERY);
        return toOrderDto(order);
    }

    public DishDto addDish(String adminEmail, DishCreateDto req) {
        Restaurant restaurant = resolveRestaurantForAdmin(adminEmail);
        Dish dish = Dish.builder()
                .name(req.name())
                .description(req.description())
                .price(req.price() == null ? 0.0 : req.price())
                .available(true)
                .category(req.category())
                .imageUrl(req.imageUrl())
                .restaurant(restaurant)
                .build();
        dish = dishRepository.save(dish);
        return toDishDto(dish);
    }

    public DishDto updateDish(String adminEmail, Long dishId, DishUpdateDto req) {
        Restaurant restaurant = resolveRestaurantForAdmin(adminEmail);
        Dish dish = dishRepository.findById(dishId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dish not found"));
        if (!dish.getRestaurant().getId().equals(restaurant.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Dish does not belong to your restaurant");
        }
        dish.setName(req.name());
        dish.setDescription(req.description());
        if (req.price() != null) dish.setPrice(req.price());
        if (req.available() != null) dish.setAvailable(req.available());
        if (req.category() != null) dish.setCategory(req.category());
        if (req.imageUrl() != null) dish.setImageUrl(req.imageUrl());
        dish = dishRepository.save(dish);
        return toDishDto(dish);
    }

    public void deleteDish(String adminEmail, Long dishId) {
        Restaurant restaurant = resolveRestaurantForAdmin(adminEmail);
        Dish dish = dishRepository.findById(dishId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dish not found"));
        if (!dish.getRestaurant().getId().equals(restaurant.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Dish does not belong to your restaurant");
        }
        dishRepository.delete(dish);
    }

    public List<CourierDto> listCouriers(String adminEmail) {
        Restaurant restaurant = resolveRestaurantForAdmin(adminEmail);
        return userRepository.findByRestaurantIdAndRole(restaurant.getId(), User.Role.COURIER).stream()
                .map(this::toCourierDto)
                .collect(Collectors.toList());
    }

    public CourierDto createCourier(String adminEmail, CourierCreateDto req) {
        Restaurant restaurant = resolveRestaurantForAdmin(adminEmail);
        if (userRepository.findByEmail(req.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User with this email already exists");
        }
        User courier = User.builder()
                .email(req.email())
                .fullName(req.fullName())
				.password(passwordEncoder.encode(req.password()))
                .role(User.Role.COURIER)
                .restaurant(restaurant)
                .build();
        courier = userRepository.save(courier);
        return toCourierDto(courier);
    }

    public CourierDto updateCourier(String adminEmail, Long courierId, CourierUpdateDto req) {
        Restaurant restaurant = resolveRestaurantForAdmin(adminEmail);
        User courier = userRepository.findByIdAndRestaurantId(courierId, restaurant.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Courier not found for your restaurant"));
        
        // Check if email is being changed and if new email is already taken
        if (!courier.getEmail().equals(req.email())) {
            if (userRepository.findByEmail(req.email()).isPresent()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User with this email already exists");
            }
        }
        
        courier.setEmail(req.email());
        courier.setFullName(req.fullName());
        courier = userRepository.save(courier);
        return toCourierDto(courier);
    }

    public void deleteCourier(String adminEmail, Long courierId) {
        Restaurant restaurant = resolveRestaurantForAdmin(adminEmail);
        User courier = userRepository.findByIdAndRestaurantId(courierId, restaurant.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Courier not found for your restaurant"));
        userRepository.delete(courier);
    }

    public RestaurantDto updateRestaurant(String adminEmail, RestaurantUpdateDto req) {
        Restaurant restaurant = resolveRestaurantForAdmin(adminEmail);
        restaurant.setName(req.name());
        restaurant.setAddress(req.address());
        restaurant.setDeliveryPrice(req.deliveryPrice() == null ? 0.0 : req.deliveryPrice());
        if (req.imageUrl() != null) restaurant.setImageUrl(req.imageUrl());
        restaurant = restaurantRepository.save(restaurant);
        return toRestaurantDto(restaurant);
    }
    
    public RestaurantDto getRestaurantForAdmin(String adminEmail) {
        Restaurant restaurant = resolveRestaurantForAdmin(adminEmail);
        return toRestaurantDto(restaurant);
    }
    
    public List<DishDto> getAllDishesForAdmin(String adminEmail) {
        Restaurant restaurant = resolveRestaurantForAdmin(adminEmail);
        List<Dish> dishes = dishRepository.findByRestaurantId(restaurant.getId());
        return dishes.stream().map(this::toDishDto).collect(Collectors.toList());
    }
    
    // Statistics methods
    public double getTodayRevenue(String adminEmail) {
        Restaurant restaurant = resolveRestaurantForAdmin(adminEmail);
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        List<Order> todayOrders = orderRepository.findByRestaurantIdAndCreatedAtAfter(restaurant.getId(), todayStart);
        return todayOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .mapToDouble(Order::getTotalPrice)
                .sum();
    }
    
    public long getTodayOrdersCount(String adminEmail) {
        Restaurant restaurant = resolveRestaurantForAdmin(adminEmail);
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        return orderRepository.countByRestaurantIdAndCreatedAtAfter(restaurant.getId(), todayStart);
    }
    
    public long getActiveOrdersCount(String adminEmail) {
        Restaurant restaurant = resolveRestaurantForAdmin(adminEmail);
        return orderRepository.countByRestaurantIdAndStatusIn(
            restaurant.getId(),
            List.of(OrderStatus.PLACED, OrderStatus.ACCEPTED, OrderStatus.COOKING, OrderStatus.READY, OrderStatus.IN_DELIVERY)
        );
    }
    
    public java.util.Map<String, Integer> getTopDishes(String adminEmail, int limit) {
        Restaurant restaurant = resolveRestaurantForAdmin(adminEmail);
        List<Order> allOrders = orderRepository.findByRestaurantId(restaurant.getId());
        
        return allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .flatMap(o -> o.getItems().stream())
                .collect(Collectors.groupingBy(
                    item -> item.getDish() != null ? item.getDish().getName() : "Unknown",
                    Collectors.summingInt(OrderItem::getQuantity)
                ))
                .entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                .limit(limit)
                .collect(Collectors.toMap(
                    java.util.Map.Entry::getKey,
                    java.util.Map.Entry::getValue,
                    (e1, e2) -> e1,
                    java.util.LinkedHashMap::new
                ));
    }

    public List<RestaurantSummaryDto> listRestaurants() {
        return restaurantRepository.findAll().stream()
                .map(r -> new RestaurantSummaryDto(
                        r.getId(),
                        r.getName(),
                        r.getAddress(),
                        BigDecimal.valueOf(r.getDeliveryPrice()),
                        r.getImageUrl(),
                        null,
                        0
                ))
                .toList();
    }

    public List<DishDto> getMenu(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));

        List<Dish> allDishes = dishRepository.findByRestaurantId(restaurant.getId());
        System.out.println("RestaurantService.getMenu: Found " + allDishes.size() + " dishes for restaurant " + restaurantId);
        
        List<Dish> availableDishes = dishRepository.findByRestaurantIdAndAvailableTrue(restaurant.getId());
        System.out.println("RestaurantService.getMenu: Found " + availableDishes.size() + " available dishes");
        
        if (availableDishes.isEmpty() && !allDishes.isEmpty()) {
            System.out.println("RestaurantService.getMenu: WARNING - No available dishes, but " + allDishes.size() + " total dishes exist!");
            System.out.println("RestaurantService.getMenu: All dishes availability: " + 
                allDishes.stream().map(d -> d.getName() + "=" + d.isAvailable()).collect(java.util.stream.Collectors.joining(", ")));
        }

        return availableDishes.stream()
                .map(d -> {
                    DishDto dto = new DishDto(
                            d.getId(),
                            d.getName(),
                            d.getDescription(),
                            d.getPrice(),
                            d.isAvailable(),
                            restaurant.getId(),
                            d.getCategory(),
                            d.getImageUrl() != null ? d.getImageUrl() : ""
                    );
                    System.out.println("RestaurantService.getMenu: Mapped dish: " + dto.name() + " (category: " + dto.category() + ")");
                    return dto;
                })
                .toList();
    }

    // ----------------- helpers / mappers -----------------

    private OrderStatus parseOrderStatus(String value) {
        try {
            return OrderStatus.valueOf(value.toUpperCase());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid order status: " + value);
        }
    }

    private void validateAdminTransition(OrderStatus current, OrderStatus target) {
        if (current == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order status is undefined");
        }
        switch (current) {
            case PLACED -> {
                if (target != OrderStatus.ACCEPTED && target != OrderStatus.CANCELLED) {
                    throw invalidTransition(current, target);
                }
            }
            case ACCEPTED -> {
                if (target != OrderStatus.COOKING && target != OrderStatus.CANCELLED) {
                    throw invalidTransition(current, target);
                }
            }
            case COOKING -> {
                if (target != OrderStatus.READY && target != OrderStatus.CANCELLED) {
                    throw invalidTransition(current, target);
                }
            }
            case READY -> {
                if (target != OrderStatus.IN_DELIVERY && target != OrderStatus.CANCELLED) {
                    throw invalidTransition(current, target);
                }
            }
            case IN_DELIVERY, DELIVERED, CANCELLED ->
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot change status once in " + current);
        }
    }

    private ResponseStatusException invalidTransition(OrderStatus from, OrderStatus to) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot change status from " + from + " to " + to);
    }

    private Restaurant resolveRestaurantForAdmin(String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin user not found"));
        if (admin.getRole() != User.Role.RESTAURANT_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not restaurant admin");
        }
        return restaurantRepository.findByAdminId(admin.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant for admin not found"));
    }

    private DishDto toDishDto(Dish dish) {
        return new DishDto(
                dish.getId(),
                dish.getName(),
                dish.getDescription(),
                dish.getPrice(),
                dish.isAvailable(),
                dish.getRestaurant() != null ? dish.getRestaurant().getId() : null,
                dish.getCategory(),
                dish.getImageUrl() != null ? dish.getImageUrl() : ""
        );
    }

    private OrderItemDto toOrderItemDto(OrderItem oi) {
        return new OrderItemDto(
                oi.getId(),
                oi.getDish() != null ? oi.getDish().getId() : null,
                oi.getDish() != null ? oi.getDish().getName() : null,
                oi.getQuantity(),
                oi.getPriceSnapshot()
        );
    }

    private OrderDto toOrderDto(Order order) {
        List<OrderItemDto> items = order.getItems() == null ? List.of() :
                order.getItems().stream().map(this::toOrderItemDto).collect(Collectors.toList());
        LocalDateTime createdAt = order.getCreatedAt();
        return new OrderDto(
                order.getId(),
                order.getStatus() != null ? order.getStatus().name() : null,
                order.getTotalPrice(),
                order.getDeliveryPrice(),
                createdAt,
                items,
                order.getUser() != null ? order.getUser().getId() : null,
                order.getUser() != null ? order.getUser().getEmail() : null,
                order.getCourier() != null ? order.getCourier().getId() : null,
                order.getCourier() != null ? order.getCourier().getEmail() : null,
                order.getCourier() != null ? order.getCourier().getFullName() : null
        );
    }

    private CourierDto toCourierDto(User u) {
        // Kurier może mieć wiele zamówień jednocześnie, więc zawsze jest dostępny
        // Można sprawdzić liczbę aktywnych dostaw, ale nie blokujemy przypisania
        long activeDeliveries = orderRepository.findByCourierIdOrderByCreatedAtDesc(u.getId()).stream()
            .filter(o -> o.getStatus() == OrderStatus.IN_DELIVERY)
            .count();
        // Kurier jest dostępny (może przyjąć więcej zamówień)
        // Można pokazać liczbę aktywnych dostaw w UI, ale nie blokujemy
        boolean isAvailable = true;
        return new CourierDto(
            u.getId(), 
            u.getEmail(), 
            u.getFullName(), 
            u.getRestaurant() != null ? u.getRestaurant().getId() : null,
            isAvailable
        );
    }

    public RestaurantDto toRestaurantDto(Restaurant r) {
        return new RestaurantDto(
                r.getId(),
                r.getName(),
                r.getAddress(),
                r.getDeliveryPrice(),
                r.getAdmin() != null ? r.getAdmin().getId() : null,
                r.getAdmin() != null ? r.getAdmin().getEmail() : null,
                r.getImageUrl()
        );
    }
}
