package pollub.eatgo.views;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import pollub.eatgo.service.AuthenticationService;

@Route("courier")
@PageTitle("EatGo - Panel Kuriera")
public class CourierDashboardView extends VerticalLayout {
    
    private final AuthenticationService authService;
    
    public CourierDashboardView(AuthenticationService authService) {
        this.authService = authService;
        
        setSizeFull();
        setSpacing(false);
        setPadding(false);
        addClassName("courier-dashboard-view");
        
        H1 title = new H1("Panel Kuriera - W budowie");
        add(title);
    }
}

