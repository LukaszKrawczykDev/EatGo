INSERT INTO users (email, password, full_name, role) VALUES
                                                         ('admin@pizza.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Admin Pizza', 'RESTAURANT_ADMIN'),
                                                         ('admin@burger.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Admin Burger', 'RESTAURANT_ADMIN'),
                                                         ('courier1@eatgo.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Kurier Jan', 'COURIER'),
                                                         ('courier2@eatgo.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Kurier Anna', 'COURIER'),
                                                         ('client1@eatgo.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Marek Klient', 'CLIENT'),
                                                         ('client2@eatgo.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Kasia Klient', 'CLIENT');

INSERT INTO restaurants (name, address, delivery_price, admin_id) VALUES
                                                                      ('Pizza Planet', 'ul. Słoneczna 5, Warszawa', 5.00, 1),
                                                                      ('Burger Town', 'ul. Kwiatowa 12, Kraków', 7.00, 2);

INSERT INTO dishes (restaurant_id, name, description, price) VALUES
                                                                 (1, 'Margherita', 'Klasyczna pizza z sosem pomidorowym i serem', 24.99),
                                                                 (1, 'Pepperoni', 'Pizza z pikantnym salami', 27.50),
                                                                 (2, 'Classic Burger', 'Wołowina, ser, sałata, pomidor', 25.00),
                                                                 (2, 'Cheeseburger', 'Wołowina, podwójny ser, sos BBQ', 28.00);

INSERT INTO addresses (user_id, city, street, postal_code) VALUES
                                                               (5, 'Warszawa', 'Zielona 10', '00-123'),
                                                               (6, 'Kraków', 'Lipowa 22', '30-555');

VALUES
    (5, 1, 3, 1, 'DELIVERED', 29.99, 5.00),
    (6, 2, 4, 2, 'IN_DELIVERY', 28.00, 7.00);

INSERT INTO order_items (order_id, dish_id, quantity, price_snapshot) VALUES
                                                                          (1, 1, 1, 24.99),
                                                                          (2, 4, 1, 28.00);

INSERT INTO reviews (order_id, reviewer_id, target_type, target_id, rating, comment)
VALUES
    (1, 5, 'RESTAURANT', 1, 5, 'Pyszna pizza!'),
    (1, 5, 'COURIER', 3, 4, 'Szybka dostawa, miły kurier.');