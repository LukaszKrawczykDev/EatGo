package pollub.eatgo.dto.review;

public record OrderReviewStatusDto(
		boolean restaurantReviewed,
		boolean courierReviewed,
		boolean canReviewRestaurant,
		boolean canReviewCourier
) {}

