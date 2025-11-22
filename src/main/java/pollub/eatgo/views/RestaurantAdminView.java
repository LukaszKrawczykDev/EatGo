package pollub.eatgo.views;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import pollub.eatgo.service.AuthenticationService;

@Route("restaurant")
@PageTitle("EatGo - Panel Restauracji")
public class RestaurantAdminView extends VerticalLayout {
    
    private final AuthenticationService authService;
    
    public RestaurantAdminView(AuthenticationService authService) {
        this.authService = authService;
        
        setSizeFull();
        setSpacing(false);
        setPadding(false);
        addClassName("restaurant-admin-view");
        
        H1 title = new H1("Panel Restauracji - W budowie");
        add(title);
    }
}

