ALTER SEQUENCE users_id_seq RESTART WITH 7;
ALTER SEQUENCE restaurants_id_seq RESTART WITH 3;
ALTER SEQUENCE dishes_id_seq RESTART WITH 5;
ALTER SEQUENCE addresses_id_seq RESTART WITH 3;
ALTER SEQUENCE orders_id_seq RESTART WITH 3;
ALTER SEQUENCE order_items_id_seq RESTART WITH 3;
ALTER SEQUENCE reviews_id_seq RESTART WITH 3;

INSERT INTO users (id, email, password, full_name, role, restaurant_id, created_at, updated_at) VALUES
                                                                                                    (1, 'admin@pizza.pl',  '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Admin Pizza',  'RESTAURANT_ADMIN', NULL, NOW(), NOW()),
                                                                                                    (2, 'admin@burger.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Admin Burger', 'RESTAURANT_ADMIN', NULL, NOW(), NOW())
    ON CONFLICT (id) DO NOTHING;

INSERT INTO restaurants (id, name, address, delivery_price, admin_id) VALUES
                                                                          (1, 'Pizza Planet', 'ul. Słoneczna 5, Warszawa', 5.00, 1),
                                                                          (2, 'Burger Town', 'ul. Kwiatowa 12, Kraków', 7.00, 2)
    ON CONFLICT (id) DO NOTHING;

INSERT INTO dishes (id, restaurant_id, name, description, price, available) VALUES
                                                                                (1, 1, 'Margherita', 'Klasyczna pizza z sosem pomidorowym i serem', 24.99, TRUE),
                                                                                (2, 1, 'Pepperoni', 'Pizza z pikantnym salami i serem mozzarella', 27.50, TRUE),
                                                                                (3, 2, 'Classic Burger', 'Wołowina, ser, sałata, pomidor, sos', 25.00, TRUE),
                                                                                (4, 2, 'Cheeseburger', 'Wołowina, podwójny ser, sos BBQ', 28.00, TRUE)
    ON CONFLICT (id) DO NOTHING;

INSERT INTO users (id, email, password, full_name, role, restaurant_id, created_at, updated_at) VALUES
                                                                                                    (3, 'courier1@eatgo.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Kurier Jan',  'COURIER', 1, NOW(), NOW()),
                                                                                                    (4, 'courier2@eatgo.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Kurier Anna', 'COURIER', 2, NOW(), NOW()),
                                                                                                    (5, 'client1@eatgo.pl',  '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Marek Klient', 'CLIENT', NULL, NOW(), NOW()),
                                                                                                    (6, 'client2@eatgo.pl',  '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Kasia Klient', 'CLIENT', NULL, NOW(), NOW())
    ON CONFLICT (id) DO NOTHING;

INSERT INTO addresses (id, user_id, city, street, postal_code) VALUES
                                                                   (1, 5, 'Warszawa', 'Zielona 10', '00-123'),
                                                                   (2, 6, 'Kraków',  'Lipowa 22', '30-555')
    ON CONFLICT (id) DO NOTHING;

INSERT INTO orders (id, user_id, restaurant_id, courier_id, address_id, status, total_price, delivery_price, created_at, updated_at) VALUES
                                                                                                                                         (1, 5, 1, 3, 1, 'DELIVERED', 29.99, 5.00, NOW(), NOW()),
                                                                                                                                         (2, 6, 2, 4, 2, 'IN_DELIVERY', 28.00, 7.00, NOW(), NOW())
    ON CONFLICT (id) DO NOTHING;

INSERT INTO order_items (id, order_id, dish_id, quantity, price_snapshot) VALUES
                                                                              (1, 1, 1, 1, 24.99),
                                                                              (2, 2, 4, 1, 28.00)
    ON CONFLICT (id) DO NOTHING;

INSERT INTO reviews (id, order_id, reviewer_id, target_type, target_id, rating, comment, created_at) VALUES
                                                                                                         (1, 1, 5, 'RESTAURANT', 1, 5, 'Pyszna pizza! Na pewno zamówię ponownie.', NOW()),
                                                                                                         (2, 1, 5, 'COURIER',    3, 4, 'Szybka dostawa, bardzo miły kurier.', NOW())
    ON CONFLICT (id) DO NOTHING;
