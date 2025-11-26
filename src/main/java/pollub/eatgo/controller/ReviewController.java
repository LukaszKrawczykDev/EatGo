package pollub.eatgo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pollub.eatgo.dto.review.ReviewCreateDto;
import pollub.eatgo.model.User;
import pollub.eatgo.repository.UserRepository;
import pollub.eatgo.service.ReviewService;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

	private final ReviewService reviewService;
	private final UserRepository userRepository;

	@PostMapping
	public ResponseEntity<Void> add(@RequestBody @Valid ReviewCreateDto body, Authentication auth) {
		Long userId = userRepository.findByEmail(auth.getName()).map(User::getId).orElseThrow();
		reviewService.addReview(userId, body);
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}

	@GetMapping("/orders/{orderId}/status")
	public pollub.eatgo.dto.review.OrderReviewStatusDto getOrderReviewStatus(
			@PathVariable Long orderId, 
			Authentication auth) {
		Long userId = userRepository.findByEmail(auth.getName()).map(User::getId).orElseThrow();
		return reviewService.getOrderReviewStatus(userId, orderId);
	}

	@GetMapping("/restaurants/{restaurantId}/can-review")
	public ResponseEntity<java.util.Map<String, Object>> canReviewRestaurant(
			@PathVariable Long restaurantId,
			Authentication auth) {
		Long userId = userRepository.findByEmail(auth.getName()).map(User::getId).orElseThrow();
		Long orderId = reviewService.findReviewableOrderForRestaurant(userId, restaurantId);
		
		java.util.Map<String, Object> response = new java.util.HashMap<>();
		response.put("canReview", orderId != null);
		response.put("orderId", orderId);
		
		return ResponseEntity.ok(response);
	}
}


