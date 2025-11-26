package pollub.eatgo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pollub.eatgo.dto.review.ReviewCreateDto;
import pollub.eatgo.dto.review.ReviewDto;
import pollub.eatgo.dto.review.ReviewTargetType;
import pollub.eatgo.model.Order;
import pollub.eatgo.model.OrderStatus;
import pollub.eatgo.model.Restaurant;
import pollub.eatgo.model.Review;
import pollub.eatgo.model.User;
import pollub.eatgo.repository.OrderRepository;
import pollub.eatgo.repository.RestaurantRepository;
import pollub.eatgo.repository.ReviewRepository;
import pollub.eatgo.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

	private final OrderRepository orderRepository;
	private final UserRepository userRepository;
	private final ReviewRepository reviewRepository;
	private final RestaurantRepository restaurantRepository;

	public void addReview(Long reviewerId, ReviewCreateDto req) {
		User reviewer = userRepository.findById(reviewerId).orElseThrow();
		Order order = orderRepository.findByIdAndUserId(req.orderId(), reviewer.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found for user"));

		if (order.getStatus() != OrderStatus.DELIVERED) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order must be DELIVERED to add review");
		}

		String targetType = req.targetType() == ReviewTargetType.RESTAURANT ? "RESTAURANT" : "COURIER";
		Long targetId = req.targetType() == ReviewTargetType.RESTAURANT
				? order.getRestaurant().getId()
				: (order.getCourier() != null ? order.getCourier().getId() : null);

		if (targetId == null && "COURIER".equals(targetType)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No courier assigned to order");
		}

		boolean exists = reviewRepository.existsByOrderIdAndReviewerIdAndTargetType(order.getId(), reviewer.getId(), targetType);
		if (exists) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Review already exists for this order and target");
		}

		Review review = Review.builder()
				.order(order)
				.reviewer(reviewer)
				.targetType(targetType)
				.targetId(targetId)
				.rating(req.rating())
				.comment(req.comment())
				.build();
		reviewRepository.save(review);
	}

	public List<ReviewDto> getReviewsForRestaurant(Long restaurantId) {
		Restaurant restaurant = restaurantRepository.findById(restaurantId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));
		return mapToDto(reviewRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc("RESTAURANT", restaurant.getId()));
	}

	public List<ReviewDto> getReviewsForAdmin(String adminEmail) {
		User admin = userRepository.findByEmail(adminEmail)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found"));
		if (admin.getRole() != User.Role.RESTAURANT_ADMIN) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not restaurant admin");
		}
		Restaurant restaurant = restaurantRepository.findByAdminId(admin.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant for admin not found"));
		return mapToDto(reviewRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc("RESTAURANT", restaurant.getId()));
	}

	public List<ReviewDto> getReviewsForCourier(Long courierId) {
		User courier = userRepository.findById(courierId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Courier not found"));
		if (courier.getRole() != User.Role.COURIER) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a courier");
		}
		return mapToDto(reviewRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc("COURIER", courierId));
	}

	public pollub.eatgo.dto.review.OrderReviewStatusDto getOrderReviewStatus(Long userId, Long orderId) {
		Order order = orderRepository.findByIdAndUserId(orderId, userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found for user"));
		
		boolean restaurantReviewed = reviewRepository.existsByOrderIdAndReviewerIdAndTargetType(orderId, userId, "RESTAURANT");
		boolean courierReviewed = order.getCourier() != null 
				? reviewRepository.existsByOrderIdAndReviewerIdAndTargetType(orderId, userId, "COURIER")
				: false;
		
		boolean canReviewRestaurant = order.getStatus() == OrderStatus.DELIVERED && !restaurantReviewed;
		boolean canReviewCourier = order.getStatus() == OrderStatus.DELIVERED 
				&& order.getCourier() != null 
				&& !courierReviewed;
		
		return new pollub.eatgo.dto.review.OrderReviewStatusDto(
				restaurantReviewed,
				courierReviewed,
				canReviewRestaurant,
				canReviewCourier
		);
	}

	public Long findReviewableOrderForRestaurant(Long userId, Long restaurantId) {
		// Znajdź dostarczone zamówienia użytkownika dla tej restauracji
		List<Order> deliveredOrders = orderRepository.findByUserIdAndRestaurantIdAndStatus(
				userId, restaurantId, OrderStatus.DELIVERED);
		
		// Znajdź pierwsze zamówienie, dla którego nie ma jeszcze recenzji restauracji
		for (Order order : deliveredOrders) {
			boolean alreadyReviewed = reviewRepository.existsByOrderIdAndReviewerIdAndTargetType(
					order.getId(), userId, "RESTAURANT");
			if (!alreadyReviewed) {
				return order.getId();
			}
		}
		
		return null; // Nie znaleziono zamówienia, które można ocenić
	}

	private List<ReviewDto> mapToDto(List<Review> reviews) {
		return reviews.stream()
				.map(r -> new ReviewDto(
						r.getId(),
						"COURIER".equals(r.getTargetType()) ? ReviewTargetType.COURIER : ReviewTargetType.RESTAURANT,
						r.getTargetId(),
						r.getRating(),
						r.getComment(),
						r.getCreatedAt(),
						r.getOrder() != null ? r.getOrder().getId() : null,
						r.getReviewer() != null ? r.getReviewer().getId() : null,
						r.getReviewer() != null ? r.getReviewer().getFullName() : null
				))
				.toList();
	}
}

