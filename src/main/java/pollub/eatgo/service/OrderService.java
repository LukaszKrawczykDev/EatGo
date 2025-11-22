package pollub.eatgo.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pollub.eatgo.dto.order.OrderCreateRequestDto;
import pollub.eatgo.dto.order.OrderDetailsDto;
import pollub.eatgo.dto.order.OrderDto;
import pollub.eatgo.dto.order.OrderItemDto;
import pollub.eatgo.model.*;
import pollub.eatgo.repository.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

	private final OrderRepository orderRepository;
	private final UserRepository userRepository;
	private final RestaurantRepository restaurantRepository;
	private final AddressRepository addressRepository;
	private final DishRepository dishRepository;

	public OrderDto createOrder(Long userId, OrderCreateRequestDto req) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
		Restaurant restaurant = restaurantRepository.findById(req.restaurantId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));
		Address address = addressRepository.findByIdAndUserId(req.addressId(), user.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Address not found for user"));

		Order order = new Order();
		order.setUser(user);
		order.setRestaurant(restaurant);
		order.setAddress(address);
		order.setStatus(OrderStatus.PLACED);

		List<OrderItem> items = new ArrayList<>();
		double itemsTotal = 0.0;
		for (var itemReq : req.items()) {
			Dish dish = dishRepository.findById(itemReq.dishId())
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dish not found"));
			if (dish.getRestaurant() == null || !dish.getRestaurant().getId().equals(restaurant.getId())) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dish does not belong to the restaurant");
			}
			if (!dish.isAvailable()) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dish " + dish.getName() + " is not available");
			}
			double linePrice = dish.getPrice() * itemReq.quantity();
			itemsTotal += linePrice;

			OrderItem oi = new OrderItem();
			oi.setOrder(order);
			oi.setDish(dish);
			oi.setQuantity(itemReq.quantity());
			oi.setPriceSnapshot(dish.getPrice());
			items.add(oi);
		}
		order.setItems(items);
		order.setDeliveryPrice(restaurant.getDeliveryPrice());
		order.setTotalPrice(itemsTotal + restaurant.getDeliveryPrice());

		order = orderRepository.save(order);
		return toOrderDto(order);
	}

	public List<OrderDto> listUserOrders(Long userId) {
		return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
				.map(this::toOrderDto)
				.collect(Collectors.toList());
	}

	public OrderDetailsDto getOrderDetails(Long userId, Long orderId) {
		Order order = orderRepository.findByIdAndUserId(orderId, userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
		return toOrderDetailsDto(order);
	}

	// ----------------- helpers -----------------

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
		return new OrderDto(
				order.getId(),
				order.getStatus() != null ? order.getStatus().name() : null,
				order.getTotalPrice(),
				order.getDeliveryPrice(),
				order.getCreatedAt(),
				items,
				order.getUser() != null ? order.getUser().getId() : null,
				order.getUser() != null ? order.getUser().getEmail() : null,
				order.getCourier() != null ? order.getCourier().getId() : null,
				order.getCourier() != null ? order.getCourier().getEmail() : null
		);
	}

	private OrderDetailsDto toOrderDetailsDto(Order order) {
		List<OrderItemDto> items = order.getItems() == null ? List.of() :
				order.getItems().stream().map(this::toOrderItemDto).collect(Collectors.toList());
		return new OrderDetailsDto(
				order.getId(),
				order.getStatus() != null ? order.getStatus().name() : null,
				BigDecimal.valueOf(order.getTotalPrice()),
				BigDecimal.valueOf(order.getDeliveryPrice()),
				order.getCreatedAt() == null ? null : OffsetDateTime.from(order.getCreatedAt().atZone(java.time.ZoneId.systemDefault())),
				new pollub.eatgo.dto.restaurant.RestaurantSummaryDto(
						order.getRestaurant().getId(),
						order.getRestaurant().getName(),
						order.getRestaurant().getAddress(),
						BigDecimal.valueOf(order.getRestaurant().getDeliveryPrice()),
						order.getRestaurant().getImageUrl()
				),
				items,
				new pollub.eatgo.dto.address.AddressDto(
						order.getAddress().getId(),
						order.getAddress().getCity(),
						order.getAddress().getStreet(),
						order.getAddress().getPostalCode(),
						order.getAddress().getApartmentNumber()
				)
		);
	}
}


