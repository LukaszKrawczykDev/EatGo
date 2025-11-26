package pollub.eatgo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pollub.eatgo.dto.order.OrderDto;
import pollub.eatgo.dto.order.OrderDetailsDto;
import pollub.eatgo.dto.order.OrderStatusUpdateDto;
import pollub.eatgo.model.Order;
import pollub.eatgo.model.OrderStatus;
import pollub.eatgo.model.User;
import pollub.eatgo.repository.OrderRepository;
import pollub.eatgo.repository.UserRepository;
import pollub.eatgo.service.OrderNotificationService;
import pollub.eatgo.service.ReviewService;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/courier")
@RequiredArgsConstructor
public class CourierController {

	private final OrderRepository orderRepository;
	private final UserRepository userRepository;
	private final ReviewService reviewService;
	private final OrderNotificationService orderNotificationService;

	@GetMapping("/orders")
	public List<OrderDetailsDto> listAssigned(Authentication auth) {
		User courier = resolveCourier(auth);
		return orderRepository.findByCourierIdOrderByCreatedAtDesc(courier.getId()).stream()
				.map(this::toOrderDetailsDto)
				.collect(Collectors.toList());
	}

	@GetMapping("/orders/{id}")
	public OrderDetailsDto getOrderDetails(@PathVariable Long id, Authentication auth) {
		User courier = resolveCourier(auth);
		Order order = orderRepository.findByIdAndCourierId(id, courier.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
		return toOrderDetailsDto(order);
	}

	@PutMapping("/orders/{id}/status")
	public ResponseEntity<OrderDto> updateStatus(@PathVariable Long id,
	                                             @RequestBody @Valid OrderStatusUpdateDto body,
	                                             Authentication auth) {
		User courier = resolveCourier(auth);
		Order order = orderRepository.findByIdAndCourierId(id, courier.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

		OrderStatus previousStatus = order.getStatus();
		OrderStatus targetStatus;
		try {
			targetStatus = parseStatus(body.status());
		} catch (IllegalArgumentException ex) {
			return ResponseEntity.badRequest().build();
		}
		if (targetStatus != OrderStatus.DELIVERED || order.getStatus() != OrderStatus.IN_DELIVERY) {
			return ResponseEntity.badRequest().build();
		}
		order.setStatus(OrderStatus.DELIVERED);
		order = orderRepository.save(order);
		orderNotificationService.addStatusChangeNotification(order, previousStatus, OrderStatus.DELIVERED);
		return ResponseEntity.ok(toOrderDto(order));
	}

	private User resolveCourier(Authentication auth) {
		String email = auth.getName();
		User courier = userRepository.findByEmail(email).orElseThrow();
		if (courier.getRole() != User.Role.COURIER) {
			throw new IllegalStateException("Authenticated user is not a courier");
		}
		return courier;
	}

	private OrderStatus parseStatus(String value) {
		try {
			return OrderStatus.valueOf(value.toUpperCase());
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid status: " + value);
		}
	}

	private OrderDto toOrderDto(Order order) {
		return new OrderDto(
				order.getId(),
				order.getStatus() != null ? order.getStatus().name() : null,
				order.getTotalPrice(),
				order.getDeliveryPrice(),
				order.getCreatedAt(),
				order.getItems() == null ? List.of() :
						order.getItems().stream()
								.map(oi -> new pollub.eatgo.dto.order.OrderItemDto(
										oi.getId(),
										oi.getDish() != null ? oi.getDish().getId() : null,
										oi.getDish() != null ? oi.getDish().getName() : null,
										oi.getQuantity(),
										oi.getPriceSnapshot()
								))
								.collect(Collectors.toList()),
				order.getUser() != null ? order.getUser().getId() : null,
				order.getUser() != null ? order.getUser().getEmail() : null,
				order.getCourier() != null ? order.getCourier().getId() : null,
				order.getCourier() != null ? order.getCourier().getEmail() : null,
				order.getCourier() != null ? order.getCourier().getFullName() : null
		);
	}

	private OrderDetailsDto toOrderDetailsDto(Order order) {
		pollub.eatgo.dto.restaurant.RestaurantSummaryDto restaurantDto = null;
		if (order.getRestaurant() != null) {
			restaurantDto = new pollub.eatgo.dto.restaurant.RestaurantSummaryDto(
					order.getRestaurant().getId(),
					order.getRestaurant().getName(),
					order.getRestaurant().getAddress(),
					BigDecimal.valueOf(order.getRestaurant().getDeliveryPrice()),
					order.getRestaurant().getImageUrl(),
					null,
					0
			);
		}

		return new OrderDetailsDto(
				order.getId(),
				order.getStatus() != null ? order.getStatus().name() : null,
				BigDecimal.valueOf(order.getTotalPrice()),
				BigDecimal.valueOf(order.getDeliveryPrice()),
				order.getCreatedAt() == null ? null : OffsetDateTime.from(order.getCreatedAt().atZone(java.time.ZoneId.systemDefault())),
				restaurantDto,
				order.getItems() == null ? List.of() :
						order.getItems().stream()
								.map(oi -> new pollub.eatgo.dto.order.OrderItemDto(
										oi.getId(),
										oi.getDish() != null ? oi.getDish().getId() : null,
										oi.getDish() != null ? oi.getDish().getName() : null,
										oi.getQuantity(),
										oi.getPriceSnapshot()
								))
								.collect(Collectors.toList()),
				order.getAddress() != null ? new pollub.eatgo.dto.address.AddressDto(
						order.getAddress().getId(),
						order.getAddress().getCity(),
						order.getAddress().getStreet(),
						order.getAddress().getPostalCode(),
						order.getAddress().getApartmentNumber()
				) : null
		);
	}
}


