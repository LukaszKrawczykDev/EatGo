package pollub.eatgo.service;

import org.springframework.stereotype.Service;
import pollub.eatgo.model.Order;
import pollub.eatgo.model.OrderStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class OrderNotificationService {

    public record OrderNotification(
            Long id,
            Long orderId,
            Long userId,
            OrderStatus previousStatus,
            OrderStatus currentStatus,
            String message,
            boolean read,
            LocalDateTime createdAt
    ) {}

    private final Map<Long, List<OrderNotification>> notificationsByUser = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    public void addStatusChangeNotification(Order order, OrderStatus previousStatus, OrderStatus currentStatus) {
        if (order == null || order.getUser() == null || previousStatus == currentStatus) {
            return;
        }
        Long userId = order.getUser().getId();
        if (userId == null) {
            return;
        }

        String message = String.format(
                "Zamówienie #%d: status zmieniony z %s na %s",
                order.getId(),
                toLabel(previousStatus),
                toLabel(currentStatus)
        );

        OrderNotification notification = new OrderNotification(
                idSequence.getAndIncrement(),
                order.getId(),
                userId,
                previousStatus,
                currentStatus,
                message,
                false,
                LocalDateTime.now()
        );

        notificationsByUser.compute(userId, (key, list) -> {
            if (list == null) {
                list = new ArrayList<>();
            } else {
                list = new ArrayList<>(list);
            }
            list.add(notification);
            list.sort(Comparator.comparing(OrderNotification::createdAt).reversed());
            if (list.size() > 50) {
                list = new ArrayList<>(list.subList(0, 50));
            }
            return list;
        });
    }

    public List<OrderNotification> getNotificationsForUser(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return notificationsByUser.getOrDefault(userId, List.of())
                .stream()
                .sorted(Comparator.comparing(OrderNotification::createdAt).reversed())
                .toList();
    }

    public long countUnread(Long userId) {
        if (userId == null) {
            return 0;
        }
        return notificationsByUser.getOrDefault(userId, List.of())
                .stream()
                .filter(n -> !n.read())
                .count();
    }

    public void markAllAsRead(Long userId) {
        if (userId == null) {
            return;
        }
        notificationsByUser.computeIfPresent(userId, (key, list) -> {
            List<OrderNotification> updated = new ArrayList<>(list.size());
            for (OrderNotification n : list) {
                if (n.read()) {
                    updated.add(n);
                } else {
                    updated.add(new OrderNotification(
                            n.id(),
                            n.orderId(),
                            n.userId(),
                            n.previousStatus(),
                            n.currentStatus(),
                            n.message(),
                            true,
                            n.createdAt()
                    ));
                }
            }
            return updated;
        });
    }

    /**
     * Usuwa wszystkie powiadomienia dla danego użytkownika.
     * Używane np. po potwierdzeniu modala „zamówienie dostarczone”.
     */
    public void clearForUser(Long userId) {
        if (userId == null) {
            return;
        }
        notificationsByUser.remove(userId);
    }

    private String toLabel(OrderStatus status) {
        if (status == null) {
            return "nieznany";
        }
        return switch (status) {
            case PLACED -> "ZŁOŻONE";
            case ACCEPTED -> "PRZYJĘTE";
            case COOKING -> "W PRZYGOTOWANIU";
            case READY -> "GOTOWE";
            case IN_DELIVERY -> "W DRODZE";
            case DELIVERED -> "DOSTARCZONE";
            case CANCELLED -> "ANULOWANE";
        };
    }
}


