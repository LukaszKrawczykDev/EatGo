package pollub.eatgo.dto.restaurant;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantAdminDto {
    private Long id;
    private String name;
    private String address;
    private double deliveryPrice;
}
