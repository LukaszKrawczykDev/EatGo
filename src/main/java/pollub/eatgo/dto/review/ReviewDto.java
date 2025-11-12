package pollub.eatgo.dto.review;

import java.time.LocalDateTime;

public record ReviewDto(
		Long id,
		ReviewTargetType targetType,
		Long targetId,
		int rating,
		String comment,
		LocalDateTime createdAt,
		Long orderId,
		Long reviewerId,
		String reviewerName
) {}


