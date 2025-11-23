package pollub.eatgo.dto.restaurant;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantDto {
    private Long id;
    private String name;
    private String address;
    private double deliveryPrice;
    private Long adminId;
    private String adminEmail;
    private String imageUrl;
}
