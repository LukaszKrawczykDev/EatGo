package pollub.eatgo.service;

import com.itextpdf.html2pdf.HtmlConverter;
import org.springframework.stereotype.Service;
import pollub.eatgo.dto.order.OrderDto;
import pollub.eatgo.dto.restaurant.RestaurantDto;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PdfService {
    
    public byte[] generateReceipt(OrderDto order, RestaurantDto restaurant) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            String html = generateReceiptHtml(order, restaurant);
            HtmlConverter.convertToPdf(html, baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF receipt", e);
        }
    }
    
    private String generateReceiptHtml(OrderDto order, RestaurantDto restaurant) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        String orderDate = order.createdAt() != null ? order.createdAt().format(formatter) : "";
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; padding: 20px; }");
        html.append("h1 { color: #333; border-bottom: 2px solid #333; padding-bottom: 10px; }");
        html.append("h2 { color: #666; margin-top: 20px; }");
        html.append("table { width: 100%; border-collapse: collapse; margin: 20px 0; }");
        html.append("th, td { padding: 10px; text-align: left; border-bottom: 1px solid #ddd; }");
        html.append("th { background-color: #f2f2f2; }");
        html.append(".total { font-weight: bold; font-size: 1.2em; margin-top: 20px; }");
        html.append(".info { margin: 10px 0; }");
        html.append("</style></head><body>");
        
        html.append("<h1>BON ZAMÓWIENIA</h1>");
        html.append("<div class='info'><strong>Restauracja:</strong> ").append(restaurant.getName()).append("</div>");
        html.append("<div class='info'><strong>Adres:</strong> ").append(restaurant.getAddress()).append("</div>");
        html.append("<div class='info'><strong>Numer zamówienia:</strong> #").append(order.id()).append("</div>");
        html.append("<div class='info'><strong>Data:</strong> ").append(orderDate).append("</div>");
        html.append("<div class='info'><strong>Status:</strong> ").append(order.status()).append("</div>");
        html.append("<div class='info'><strong>Klient:</strong> ").append(order.userEmail()).append("</div>");
        
        if (order.courierFullName() != null) {
            html.append("<div class='info'><strong>Kurier:</strong> ").append(order.courierFullName()).append("</div>");
        }
        
        html.append("<h2>Produkty:</h2>");
        html.append("<table>");
        html.append("<tr><th>Nazwa</th><th>Ilość</th><th>Cena jednostkowa</th><th>Wartość</th></tr>");
        
        if (order.items() != null) {
            for (var item : order.items()) {
                html.append("<tr>");
                html.append("<td>").append(item.dishName() != null ? item.dishName() : "N/A").append("</td>");
                html.append("<td>").append(item.quantity()).append("</td>");
                html.append("<td>").append(String.format("%.2f zł", item.priceSnapshot())).append("</td>");
                html.append("<td>").append(String.format("%.2f zł", item.priceSnapshot() * item.quantity())).append("</td>");
                html.append("</tr>");
            }
        }
        
        html.append("</table>");
        html.append("<div class='total'>");
        html.append("<div>Dostawa: ").append(String.format("%.2f zł", order.deliveryPrice())).append("</div>");
        html.append("<div>SUMA: ").append(String.format("%.2f zł", order.totalPrice())).append("</div>");
        html.append("</div>");
        
        html.append("</body></html>");
        return html.toString();
    }
}

