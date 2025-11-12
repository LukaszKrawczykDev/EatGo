package pollub.eatgo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pollub.eatgo.dto.order.OrderCreateRequestDto;
import pollub.eatgo.dto.order.OrderDetailsDto;
import pollub.eatgo.dto.order.OrderDto;
import pollub.eatgo.service.OrderService;
import pollub.eatgo.repository.UserRepository;
import pollub.eatgo.model.User;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

	private final OrderService orderService;
	private final UserRepository userRepository;

	@PostMapping
	public ResponseEntity<OrderDto> create(@RequestBody @Valid OrderCreateRequestDto body, Authentication auth) {
		OrderDto created = orderService.createOrder(resolveUserId(auth), body);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@GetMapping
	public List<OrderDto> list(Authentication auth) {
		return orderService.listUserOrders(resolveUserId(auth));
	}

	@GetMapping("/{id}")
	public OrderDetailsDto details(@PathVariable Long id, Authentication auth) {
		return orderService.getOrderDetails(resolveUserId(auth), id);
	}

	private Long resolveUserId(Authentication auth) {
		String email = auth.getName();
		User u = userRepository.findByEmail(email).orElseThrow();
		return u.getId();
	}
}


