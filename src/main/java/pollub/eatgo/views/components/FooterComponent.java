package pollub.eatgo.views.components;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;

public class FooterComponent extends Div {
    
    public FooterComponent() {
        addClassName("footer");
        setWidthFull();
        
        Div footerContent = new Div();
        footerContent.addClassName("footer-content");
        footerContent.setWidthFull();
        
        VerticalLayout footerLayout = new VerticalLayout();
        footerLayout.setSpacing(true);
        footerLayout.setPadding(false);
        footerLayout.setWidthFull();
        
        Hr divider = new Hr();
        divider.addClassName("footer-divider");
        
        HorizontalLayout footerInfo = new HorizontalLayout();
        footerInfo.setSpacing(true);
        footerInfo.setAlignItems(Alignment.CENTER);
        footerInfo.setJustifyContentMode(JustifyContentMode.CENTER);
        footerInfo.setWidthFull();
        footerInfo.addClassName("footer-info");
        
        Span copyright = new Span("© 2025 EatGo. Wszystkie prawa zastrzeżone.");
        copyright.addClassName("footer-copyright");
        
        HorizontalLayout footerLinks = new HorizontalLayout();
        footerLinks.setSpacing(true);
        footerLinks.setAlignItems(Alignment.CENTER);
        footerLinks.addClassName("footer-links");
        
        Paragraph about = new Paragraph("O nas");
        about.addClassName("footer-link");
        about.addClickListener(e -> {
            // TODO: Navigate to about page
        });
        
        Paragraph contact = new Paragraph("Kontakt");
        contact.addClassName("footer-link");
        contact.addClickListener(e -> {
            // TODO: Navigate to contact page
        });
        
        Paragraph privacy = new Paragraph("Polityka prywatności");
        privacy.addClassName("footer-link");
        privacy.addClickListener(e -> {
            // TODO: Navigate to privacy page
        });
        
        footerLinks.add(about, contact, privacy);
        footerInfo.add(copyright, footerLinks);
        
        footerLayout.add(divider, footerInfo);
        footerContent.add(footerLayout);
        add(footerContent);
    }
}

