-- Usuń wszystkie stare dane używając TRUNCATE CASCADE (rozwiązuje problem cyklicznych zależności)
-- CASCADE automatycznie usuwa zależne rekordy w odpowiedniej kolejności
TRUNCATE TABLE reviews CASCADE;
TRUNCATE TABLE order_items CASCADE;
TRUNCATE TABLE orders CASCADE;
TRUNCATE TABLE dishes CASCADE;
TRUNCATE TABLE addresses CASCADE;
TRUNCATE TABLE restaurants CASCADE;
TRUNCATE TABLE users CASCADE;

-- Resetuj sekwencje
ALTER SEQUENCE users_id_seq RESTART WITH 1;
ALTER SEQUENCE restaurants_id_seq RESTART WITH 1;
ALTER SEQUENCE dishes_id_seq RESTART WITH 1;
ALTER SEQUENCE addresses_id_seq RESTART WITH 1;
ALTER SEQUENCE orders_id_seq RESTART WITH 1;
ALTER SEQUENCE order_items_id_seq RESTART WITH 1;
ALTER SEQUENCE reviews_id_seq RESTART WITH 1;

-- WARSZAWA - 2 restauracje
INSERT INTO users (id, email, password, full_name, role, created_at, updated_at) VALUES
(1, 'admin@warszawa1.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Admin Warszawa 1', 'RESTAURANT_ADMIN', NOW(), NOW()),
(2, 'admin@warszawa2.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Admin Warszawa 2', 'RESTAURANT_ADMIN', NOW(), NOW());

INSERT INTO restaurants (id, name, address, delivery_price, admin_id, image_url) VALUES
(1, 'Pizza Express Warszawa', 'ul. Marszałkowska 10, Warszawa', 5.50, 1, '/images/restaurant1.jpg'),
(2, 'Burger House Warszawa', 'ul. Nowy Świat 15, Warszawa', 6.00, 2, '/images/restaurant2.jpg');

INSERT INTO users (id, email, password, full_name, role, restaurant_id, created_at, updated_at) VALUES
(3, 'courier@warszawa1.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Kurier Warszawa 1', 'COURIER', 1, NOW(), NOW()),
(4, 'courier@warszawa2.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Kurier Warszawa 2', 'COURIER', 2, NOW(), NOW());

INSERT INTO dishes (id, restaurant_id, name, description, price, available, category) VALUES
-- Pizza Express Warszawa
(1, 1, 'Margherita', 'Klasyczna pizza z sosem pomidorowym i mozzarellą', 24.99, TRUE, 'PIZZA'),
(2, 1, 'Pepperoni', 'Pizza z pikantnym salami pepperoni', 27.50, TRUE, 'PIZZA'),
-- Burger House Warszawa
(3, 2, 'Classic Burger', 'Wołowina, ser, sałata, pomidor, sos', 25.00, TRUE, 'BURGER'),
(4, 2, 'Cheeseburger', 'Wołowina, podwójny ser, sos BBQ', 28.00, TRUE, 'BURGER');

-- LUBLIN - 15 restauracji
INSERT INTO users (id, email, password, full_name, role, created_at, updated_at) VALUES
(5, 'admin@lublin1.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Admin Lublin 1', 'RESTAURANT_ADMIN', NOW(), NOW()),
(6, 'admin@lublin2.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Admin Lublin 2', 'RESTAURANT_ADMIN', NOW(), NOW()),
(7, 'admin@lublin3.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Admin Lublin 3', 'RESTAURANT_ADMIN', NOW(), NOW()),
(8, 'admin@lublin4.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Admin Lublin 4', 'RESTAURANT_ADMIN', NOW(), NOW()),
(9, 'admin@lublin5.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Admin Lublin 5', 'RESTAURANT_ADMIN', NOW(), NOW()),
(10, 'admin@lublin6.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Admin Lublin 6', 'RESTAURANT_ADMIN', NOW(), NOW()),
(11, 'admin@lublin7.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Admin Lublin 7', 'RESTAURANT_ADMIN', NOW(), NOW()),
(12, 'admin@lublin8.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Admin Lublin 8', 'RESTAURANT_ADMIN', NOW(), NOW()),
(13, 'admin@lublin9.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Admin Lublin 9', 'RESTAURANT_ADMIN', NOW(), NOW()),
(14, 'admin@lublin10.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Admin Lublin 10', 'RESTAURANT_ADMIN', NOW(), NOW()),
(15, 'admin@lublin11.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Admin Lublin 11', 'RESTAURANT_ADMIN', NOW(), NOW()),
(16, 'admin@lublin12.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Admin Lublin 12', 'RESTAURANT_ADMIN', NOW(), NOW()),
(17, 'admin@lublin13.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Admin Lublin 13', 'RESTAURANT_ADMIN', NOW(), NOW()),
(18, 'admin@lublin14.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Admin Lublin 14', 'RESTAURANT_ADMIN', NOW(), NOW()),
(19, 'admin@lublin15.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Admin Lublin 15', 'RESTAURANT_ADMIN', NOW(), NOW());

INSERT INTO restaurants (id, name, address, delivery_price, admin_id, image_url) VALUES
(3, 'Pizzeria Bella Lublin', 'ul. Krakowskie Przedmieście 1, Lublin', 4.50, 5, '/images/restaurant1.jpg'),
(4, 'Burger King Lublin', 'ul. Grodzka 5, Lublin', 5.00, 6, '/images/restaurant2.jpg'),
(5, 'Sushi Master Lublin', 'ul. Lubartowska 10, Lublin', 6.50, 7, '/images/restaurant3.jpg'),
(6, 'Kebab House Lublin', 'ul. Narutowicza 15, Lublin', 4.00, 8, '/images/restaurant4.jpg'),
(7, 'Taco Loco Lublin', 'ul. Zamojska 20, Lublin', 5.50, 9, '/images/restaurant5.jpg'),
(8, 'Pizza & Burger Lublin', 'ul. Lipowa 25, Lublin', 5.00, 10, '/images/restaurant1.jpg'),
(9, 'Asian Fusion Lublin', 'ul. Królewska 30, Lublin', 6.00, 11, '/images/restaurant2.jpg'),
(10, 'Italian Corner Lublin', 'ul. Bernardyńska 35, Lublin', 5.50, 12, '/images/restaurant3.jpg'),
(11, 'Fast Food Express Lublin', 'ul. Spokojna 40, Lublin', 4.50, 13, '/images/restaurant4.jpg'),
(12, 'Mexican Fiesta Lublin', 'ul. Wieniawska 45, Lublin', 5.00, 14, '/images/restaurant5.jpg'),
(13, 'Pizza Palace Lublin', 'ul. Chopina 50, Lublin', 4.50, 15, '/images/restaurant1.jpg'),
(14, 'Burger Paradise Lublin', 'ul. Słowackiego 55, Lublin', 5.50, 16, '/images/restaurant2.jpg'),
(15, 'Sushi World Lublin', 'ul. Racławickie 60, Lublin', 6.50, 17, '/images/restaurant3.jpg'),
(16, 'Kebab Express Lublin', 'ul. Zana 65, Lublin', 4.00, 18, '/images/restaurant4.jpg'),
(17, 'Pizza Corner Lublin', 'ul. Orla 70, Lublin', 4.50, 19, '/images/restaurant5.jpg');

-- Kurierzy dla Lublina
INSERT INTO users (id, email, password, full_name, role, restaurant_id, created_at, updated_at) VALUES
(20, 'courier@lublin1.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Kurier Lublin 1', 'COURIER', 3, NOW(), NOW()),
(21, 'courier@lublin2.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Kurier Lublin 2', 'COURIER', 4, NOW(), NOW()),
(22, 'courier@lublin3.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Kurier Lublin 3', 'COURIER', 5, NOW(), NOW()),
(23, 'courier@lublin4.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Kurier Lublin 4', 'COURIER', 6, NOW(), NOW()),
(24, 'courier@lublin5.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Kurier Lublin 5', 'COURIER', 7, NOW(), NOW()),
(25, 'courier@lublin6.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Kurier Lublin 6', 'COURIER', 8, NOW(), NOW()),
(26, 'courier@lublin7.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Kurier Lublin 7', 'COURIER', 9, NOW(), NOW()),
(27, 'courier@lublin8.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Kurier Lublin 8', 'COURIER', 10, NOW(), NOW()),
(28, 'courier@lublin9.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Kurier Lublin 9', 'COURIER', 11, NOW(), NOW()),
(29, 'courier@lublin10.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Kurier Lublin 10', 'COURIER', 12, NOW(), NOW()),
(30, 'courier@lublin11.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Kurier Lublin 11', 'COURIER', 13, NOW(), NOW()),
(31, 'courier@lublin12.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Kurier Lublin 12', 'COURIER', 14, NOW(), NOW()),
(32, 'courier@lublin13.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Kurier Lublin 13', 'COURIER', 15, NOW(), NOW()),
(33, 'courier@lublin14.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Kurier Lublin 14', 'COURIER', 16, NOW(), NOW()),
(34, 'courier@lublin15.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Kurier Lublin 15', 'COURIER', 17, NOW(), NOW());

-- Danie dla restauracji w Lublinie (po 10+ dań w każdej)
INSERT INTO dishes (id, restaurant_id, name, description, price, available, category) VALUES
-- Pizzeria Bella Lublin (PIZZA)
(5, 3, 'Margherita', 'Klasyczna pizza z sosem pomidorowym i mozzarellą', 22.99, TRUE, 'PIZZA'),
(6, 3, 'Pepperoni', 'Pizza z pikantnym salami pepperoni', 25.50, TRUE, 'PIZZA'),
(7, 3, 'Capricciosa', 'Pizza z szynką, pieczarkami, oliwkami', 27.99, TRUE, 'PIZZA'),
(8, 3, 'Quattro Stagioni', 'Pizza z 4 różnymi dodatkami', 29.99, TRUE, 'PIZZA'),
(9, 3, 'Hawaii', 'Pizza z szynką i ananasem', 26.50, TRUE, 'PIZZA'),
(10, 3, 'Diavola', 'Pizza z pikantną salami i papryką', 28.50, TRUE, 'PIZZA'),
(11, 3, 'Funghi', 'Pizza z pieczarkami', 24.99, TRUE, 'PIZZA'),
(12, 3, 'Prosciutto', 'Pizza z szynką parmeńską', 29.99, TRUE, 'PIZZA'),
(13, 3, 'Quattro Formaggi', 'Pizza z 4 rodzajami sera', 30.99, TRUE, 'PIZZA'),
(14, 3, 'Vegetariana', 'Pizza z warzywami', 25.99, TRUE, 'PIZZA'),
(15, 3, 'Calzone', 'Zapiekana pizza', 28.99, TRUE, 'PIZZA'),

-- Burger King Lublin (BURGER)
(16, 4, 'Classic Burger', 'Wołowina, ser, sałata, pomidor, sos', 23.00, TRUE, 'BURGER'),
(17, 4, 'Cheeseburger', 'Wołowina, podwójny ser, sos BBQ', 26.00, TRUE, 'BURGER'),
(18, 4, 'Bacon Burger', 'Wołowina, bekon, ser, sos', 28.00, TRUE, 'BURGER'),
(19, 4, 'Chicken Burger', 'Kurczak, sałata, pomidor, sos', 24.00, TRUE, 'BURGER'),
(20, 4, 'Veggie Burger', 'Warzywny burger z serem', 22.00, TRUE, 'BURGER'),
(21, 4, 'Double Burger', 'Podwójna wołowina, podwójny ser', 32.00, TRUE, 'BURGER'),
(22, 4, 'BBQ Burger', 'Wołowina, bekon, cebula, sos BBQ', 29.00, TRUE, 'BURGER'),
(23, 4, 'Spicy Burger', 'Pikantna wołowina, jalapeño, ser', 27.00, TRUE, 'BURGER'),
(24, 4, 'Mushroom Burger', 'Wołowina, pieczarki, ser', 26.00, TRUE, 'BURGER'),
(25, 4, 'Fish Burger', 'Ryba, sałata, sos tartar', 25.00, TRUE, 'BURGER'),
(26, 4, 'Turkey Burger', 'Indyk, sałata, pomidor', 24.00, TRUE, 'BURGER'),

-- Sushi Master Lublin (SUSHI)
(27, 5, 'California Roll', 'Krab, awokado, ogórek', 32.00, TRUE, 'SUSHI'),
(28, 5, 'Salmon Roll', 'Łosoś, awokado, ogórek', 35.00, TRUE, 'SUSHI'),
(29, 5, 'Tuna Roll', 'Tuńczyk, awokado', 34.00, TRUE, 'SUSHI'),
(30, 5, 'Dragon Roll', 'Krewetka tempura, awokado', 38.00, TRUE, 'SUSHI'),
(31, 5, 'Spicy Tuna Roll', 'Pikantny tuńczyk, ogórek', 36.00, TRUE, 'SUSHI'),
(32, 5, 'Eel Roll', 'Węgorz, ogórek', 40.00, TRUE, 'SUSHI'),
(33, 5, 'Rainbow Roll', 'Mieszanka ryb, awokado', 42.00, TRUE, 'SUSHI'),
(34, 5, 'Philadelphia Roll', 'Łosoś, ser śmietankowy', 37.00, TRUE, 'SUSHI'),
(35, 5, 'Tempura Roll', 'Krewetka tempura, warzywa', 39.00, TRUE, 'SUSHI'),
(36, 5, 'Cucumber Roll', 'Ogórek, sezam', 18.00, TRUE, 'SUSHI'),
(37, 5, 'Avocado Roll', 'Awokado, sezam', 20.00, TRUE, 'SUSHI'),

-- Kebab House Lublin (KEBAB)
(38, 6, 'Kebab w bułce', 'Mięso, warzywa, sosy', 18.00, TRUE, 'KEBAB'),
(39, 6, 'Kebab w picie', 'Mięso, warzywa, sosy w picie', 19.00, TRUE, 'KEBAB'),
(40, 6, 'Kebab na talerzu', 'Mięso, frytki, warzywa, sosy', 22.00, TRUE, 'KEBAB'),
(41, 6, 'Kurczak Kebab', 'Kurczak, warzywa, sosy', 20.00, TRUE, 'KEBAB'),
(42, 6, 'Wege Kebab', 'Warzywa, sosy', 16.00, TRUE, 'KEBAB'),
(43, 6, 'Mix Kebab', 'Mięso i kurczak, warzywa', 21.00, TRUE, 'KEBAB'),
(44, 6, 'Kebab XXL', 'Duży kebab z dodatkami', 25.00, TRUE, 'KEBAB'),
(45, 6, 'Kebab z serem', 'Mięso, ser, warzywa', 20.00, TRUE, 'KEBAB'),
(46, 6, 'Kebab z frytkami', 'Kebab i frytki', 23.00, TRUE, 'KEBAB'),
(47, 6, 'Kebab Family', 'Duży kebab dla rodziny', 45.00, TRUE, 'KEBAB'),
(48, 6, 'Kebab z dodatkami', 'Kebab z extra dodatkami', 24.00, TRUE, 'KEBAB'),

-- Taco Loco Lublin (MEXICAN)
(49, 7, 'Taco z kurczakiem', 'Kurczak, warzywa, sosy', 15.00, TRUE, 'MEXICAN'),
(50, 7, 'Taco z wołowiną', 'Wołowina, warzywa, sosy', 16.00, TRUE, 'MEXICAN'),
(51, 7, 'Taco z rybą', 'Ryba, warzywa, sosy', 17.00, TRUE, 'MEXICAN'),
(52, 7, 'Burrito', 'Mięso, ryż, fasola, warzywa', 22.00, TRUE, 'MEXICAN'),
(53, 7, 'Quesadilla', 'Ser, mięso, warzywa', 20.00, TRUE, 'MEXICAN'),
(54, 7, 'Nachos', 'Chipsy, ser, sosy', 18.00, TRUE, 'MEXICAN'),
(55, 7, 'Fajitas', 'Mięso, warzywa, tortille', 25.00, TRUE, 'MEXICAN'),
(56, 7, 'Enchiladas', 'Tortille z mięsem i sosem', 24.00, TRUE, 'MEXICAN'),
(57, 7, 'Chili con Carne', 'Mięso, fasola, sos', 23.00, TRUE, 'MEXICAN'),
(58, 7, 'Guacamole', 'Awokado, pomidory, cebula', 12.00, TRUE, 'MEXICAN'),
(59, 7, 'Taco Box', '3 różne taco', 42.00, TRUE, 'MEXICAN'),

-- Pizza & Burger Lublin (PIZZA, BURGER)
(60, 8, 'Margherita', 'Klasyczna pizza', 22.99, TRUE, 'PIZZA'),
(61, 8, 'Pepperoni', 'Pizza z pepperoni', 25.50, TRUE, 'PIZZA'),
(62, 8, 'Classic Burger', 'Wołowina, ser, warzywa', 23.00, TRUE, 'BURGER'),
(63, 8, 'Cheeseburger', 'Wołowina, podwójny ser', 26.00, TRUE, 'BURGER'),
(64, 8, 'Capricciosa', 'Pizza z szynką i pieczarkami', 27.99, TRUE, 'PIZZA'),
(65, 8, 'Bacon Burger', 'Wołowina, bekon, ser', 28.00, TRUE, 'BURGER'),
(66, 8, 'Hawaii', 'Pizza z szynką i ananasem', 26.50, TRUE, 'PIZZA'),
(67, 8, 'Chicken Burger', 'Kurczak, warzywa', 24.00, TRUE, 'BURGER'),
(68, 8, 'Quattro Formaggi', 'Pizza z 4 serami', 30.99, TRUE, 'PIZZA'),
(69, 8, 'Double Burger', 'Podwójna wołowina', 32.00, TRUE, 'BURGER'),
(70, 8, 'Diavola', 'Pikantna pizza', 28.50, TRUE, 'PIZZA'),

-- Asian Fusion Lublin (ASIAN)
(71, 9, 'Pad Thai', 'Makaron z kurczakiem i warzywami', 28.00, TRUE, 'ASIAN'),
(72, 9, 'Kurczak po chińsku', 'Kurczak w sosie słodko-kwaśnym', 26.00, TRUE, 'ASIAN'),
(73, 9, 'Ramen', 'Zupa z makaronem i mięsem', 32.00, TRUE, 'ASIAN'),
(74, 9, 'Kung Pao Chicken', 'Kurczak z orzechami i papryką', 29.00, TRUE, 'ASIAN'),
(75, 9, 'Sweet and Sour Pork', 'Wieprzowina w sosie słodko-kwaśnym', 27.00, TRUE, 'ASIAN'),
(76, 9, 'Beef Teriyaki', 'Wołowina w sosie teriyaki', 30.00, TRUE, 'ASIAN'),
(77, 9, 'Spring Rolls', 'Smażone roladki z warzywami', 18.00, TRUE, 'ASIAN'),
(78, 9, 'Duck with Orange', 'Kaczka w sosie pomarańczowym', 38.00, TRUE, 'ASIAN'),
(79, 9, 'Chow Mein', 'Makaron smażony z warzywami', 24.00, TRUE, 'ASIAN'),
(80, 9, 'General Tso Chicken', 'Kurczak w pikantnym sosie', 28.00, TRUE, 'ASIAN'),
(81, 9, 'Mongolian Beef', 'Wołowina z cebulą i papryką', 31.00, TRUE, 'ASIAN'),

-- Italian Corner Lublin (ITALIAN)
(82, 10, 'Spaghetti Carbonara', 'Makaron z boczkiem i jajkiem', 32.00, TRUE, 'ITALIAN'),
(83, 10, 'Penne Arrabbiata', 'Makaron z pikantnym sosem', 28.00, TRUE, 'ITALIAN'),
(84, 10, 'Lasagne', 'Warstwowy makaron z mięsem', 35.00, TRUE, 'ITALIAN'),
(85, 10, 'Risotto ai Funghi', 'Ryż z pieczarkami', 30.00, TRUE, 'ITALIAN'),
(86, 10, 'Fettuccine Alfredo', 'Makaron w sosie śmietanowym', 29.00, TRUE, 'ITALIAN'),
(87, 10, 'Pizza Margherita', 'Klasyczna pizza', 24.99, TRUE, 'ITALIAN'),
(88, 10, 'Gnocchi', 'Kluski ziemniaczane z sosem', 27.00, TRUE, 'ITALIAN'),
(89, 10, 'Osso Buco', 'Golonka cielęca z risotto', 42.00, TRUE, 'ITALIAN'),
(90, 10, 'Bruschetta', 'Grillowany chleb z pomidorami', 16.00, TRUE, 'ITALIAN'),
(91, 10, 'Tiramisu', 'Deser kawowy', 18.00, TRUE, 'ITALIAN'),
(92, 10, 'Cannoli', 'Rurki z kremem', 15.00, TRUE, 'ITALIAN'),

-- Fast Food Express Lublin (BURGER, KEBAB)
(93, 11, 'Classic Burger', 'Wołowina, ser, warzywa', 22.00, TRUE, 'BURGER'),
(94, 11, 'Cheeseburger', 'Wołowina, podwójny ser', 25.00, TRUE, 'BURGER'),
(95, 11, 'Kebab w bułce', 'Mięso, warzywa, sosy', 17.00, TRUE, 'KEBAB'),
(96, 11, 'Kebab w picie', 'Mięso, warzywa w picie', 18.00, TRUE, 'KEBAB'),
(97, 11, 'Bacon Burger', 'Wołowina, bekon, ser', 27.00, TRUE, 'BURGER'),
(98, 11, 'Kebab na talerzu', 'Mięso, frytki, warzywa', 21.00, TRUE, 'KEBAB'),
(99, 11, 'Chicken Burger', 'Kurczak, warzywa', 23.00, TRUE, 'BURGER'),
(100, 11, 'Mix Kebab', 'Mięso i kurczak', 20.00, TRUE, 'KEBAB'),
(101, 11, 'Double Burger', 'Podwójna wołowina', 30.00, TRUE, 'BURGER'),
(102, 11, 'Kebab XXL', 'Duży kebab', 24.00, TRUE, 'KEBAB'),
(103, 11, 'Spicy Burger', 'Pikantna wołowina', 26.00, TRUE, 'BURGER'),

-- Mexican Fiesta Lublin (MEXICAN)
(104, 12, 'Taco z kurczakiem', 'Kurczak, warzywa, sosy', 14.00, TRUE, 'MEXICAN'),
(105, 12, 'Taco z wołowiną', 'Wołowina, warzywa, sosy', 15.00, TRUE, 'MEXICAN'),
(106, 12, 'Burrito', 'Mięso, ryż, fasola', 21.00, TRUE, 'MEXICAN'),
(107, 12, 'Quesadilla', 'Ser, mięso, warzywa', 19.00, TRUE, 'MEXICAN'),
(108, 12, 'Nachos', 'Chipsy, ser, sosy', 17.00, TRUE, 'MEXICAN'),
(109, 12, 'Fajitas', 'Mięso, warzywa, tortille', 24.00, TRUE, 'MEXICAN'),
(110, 12, 'Enchiladas', 'Tortille z mięsem', 23.00, TRUE, 'MEXICAN'),
(111, 12, 'Chili con Carne', 'Mięso, fasola, sos', 22.00, TRUE, 'MEXICAN'),
(112, 12, 'Taco Box', '3 różne taco', 40.00, TRUE, 'MEXICAN'),
(113, 12, 'Guacamole', 'Awokado, pomidory', 11.00, TRUE, 'MEXICAN'),
(114, 12, 'Taco z rybą', 'Ryba, warzywa, sosy', 16.00, TRUE, 'MEXICAN'),

-- Pizza Palace Lublin (PIZZA)
(115, 13, 'Margherita', 'Klasyczna pizza', 21.99, TRUE, 'PIZZA'),
(116, 13, 'Pepperoni', 'Pizza z pepperoni', 24.50, TRUE, 'PIZZA'),
(117, 13, 'Capricciosa', 'Pizza z szynką i pieczarkami', 26.99, TRUE, 'PIZZA'),
(118, 13, 'Quattro Stagioni', 'Pizza z 4 dodatkami', 28.99, TRUE, 'PIZZA'),
(119, 13, 'Hawaii', 'Pizza z szynką i ananasem', 25.50, TRUE, 'PIZZA'),
(120, 13, 'Diavola', 'Pikantna pizza', 27.50, TRUE, 'PIZZA'),
(121, 13, 'Funghi', 'Pizza z pieczarkami', 23.99, TRUE, 'PIZZA'),
(122, 13, 'Prosciutto', 'Pizza z szynką parmeńską', 28.99, TRUE, 'PIZZA'),
(123, 13, 'Quattro Formaggi', 'Pizza z 4 serami', 29.99, TRUE, 'PIZZA'),
(124, 13, 'Vegetariana', 'Pizza z warzywami', 24.99, TRUE, 'PIZZA'),
(125, 13, 'Calzone', 'Zapiekana pizza', 27.99, TRUE, 'PIZZA'),

-- Burger Paradise Lublin (BURGER)
(126, 14, 'Classic Burger', 'Wołowina, ser, warzywa', 24.00, TRUE, 'BURGER'),
(127, 14, 'Cheeseburger', 'Wołowina, podwójny ser', 27.00, TRUE, 'BURGER'),
(128, 14, 'Bacon Burger', 'Wołowina, bekon, ser', 29.00, TRUE, 'BURGER'),
(129, 14, 'Chicken Burger', 'Kurczak, warzywa', 25.00, TRUE, 'BURGER'),
(130, 14, 'Veggie Burger', 'Warzywny burger', 23.00, TRUE, 'BURGER'),
(131, 14, 'Double Burger', 'Podwójna wołowina', 33.00, TRUE, 'BURGER'),
(132, 14, 'BBQ Burger', 'Wołowina, bekon, sos BBQ', 30.00, TRUE, 'BURGER'),
(133, 14, 'Spicy Burger', 'Pikantna wołowina', 28.00, TRUE, 'BURGER'),
(134, 14, 'Mushroom Burger', 'Wołowina, pieczarki', 27.00, TRUE, 'BURGER'),
(135, 14, 'Fish Burger', 'Ryba, warzywa', 26.00, TRUE, 'BURGER'),
(136, 14, 'Turkey Burger', 'Indyk, warzywa', 25.00, TRUE, 'BURGER'),

-- Sushi World Lublin (SUSHI)
(137, 15, 'California Roll', 'Krab, awokado, ogórek', 31.00, TRUE, 'SUSHI'),
(138, 15, 'Salmon Roll', 'Łosoś, awokado', 34.00, TRUE, 'SUSHI'),
(139, 15, 'Tuna Roll', 'Tuńczyk, awokado', 33.00, TRUE, 'SUSHI'),
(140, 15, 'Dragon Roll', 'Krewetka tempura', 37.00, TRUE, 'SUSHI'),
(141, 15, 'Spicy Tuna Roll', 'Pikantny tuńczyk', 35.00, TRUE, 'SUSHI'),
(142, 15, 'Eel Roll', 'Węgorz, ogórek', 39.00, TRUE, 'SUSHI'),
(143, 15, 'Rainbow Roll', 'Mieszanka ryb', 41.00, TRUE, 'SUSHI'),
(144, 15, 'Philadelphia Roll', 'Łosoś, ser śmietankowy', 36.00, TRUE, 'SUSHI'),
(145, 15, 'Tempura Roll', 'Krewetka tempura', 38.00, TRUE, 'SUSHI'),
(146, 15, 'Cucumber Roll', 'Ogórek, sezam', 17.00, TRUE, 'SUSHI'),
(147, 15, 'Avocado Roll', 'Awokado, sezam', 19.00, TRUE, 'SUSHI'),

-- Kebab Express Lublin (KEBAB)
(148, 16, 'Kebab w bułce', 'Mięso, warzywa, sosy', 17.00, TRUE, 'KEBAB'),
(149, 16, 'Kebab w picie', 'Mięso, warzywa w picie', 18.00, TRUE, 'KEBAB'),
(150, 16, 'Kebab na talerzu', 'Mięso, frytki, warzywa', 21.00, TRUE, 'KEBAB'),
(151, 16, 'Kurczak Kebab', 'Kurczak, warzywa', 19.00, TRUE, 'KEBAB'),
(152, 16, 'Wege Kebab', 'Warzywa, sosy', 15.00, TRUE, 'KEBAB'),
(153, 16, 'Mix Kebab', 'Mięso i kurczak', 20.00, TRUE, 'KEBAB'),
(154, 16, 'Kebab XXL', 'Duży kebab', 24.00, TRUE, 'KEBAB'),
(155, 16, 'Kebab z serem', 'Mięso, ser, warzywa', 19.00, TRUE, 'KEBAB'),
(156, 16, 'Kebab z frytkami', 'Kebab i frytki', 22.00, TRUE, 'KEBAB'),
(157, 16, 'Kebab Family', 'Duży kebab dla rodziny', 44.00, TRUE, 'KEBAB'),
(158, 16, 'Kebab z dodatkami', 'Kebab z extra dodatkami', 23.00, TRUE, 'KEBAB'),

-- Pizza Corner Lublin (PIZZA)
(159, 17, 'Margherita', 'Klasyczna pizza', 22.99, TRUE, 'PIZZA'),
(160, 17, 'Pepperoni', 'Pizza z pepperoni', 25.50, TRUE, 'PIZZA'),
(161, 17, 'Capricciosa', 'Pizza z szynką i pieczarkami', 27.99, TRUE, 'PIZZA'),
(162, 17, 'Quattro Stagioni', 'Pizza z 4 dodatkami', 29.99, TRUE, 'PIZZA'),
(163, 17, 'Hawaii', 'Pizza z szynką i ananasem', 25.50, TRUE, 'PIZZA'),
(164, 17, 'Diavola', 'Pikantna pizza', 27.50, TRUE, 'PIZZA'),
(165, 17, 'Funghi', 'Pizza z pieczarkami', 23.99, TRUE, 'PIZZA'),
(166, 17, 'Prosciutto', 'Pizza z szynką parmeńską', 28.99, TRUE, 'PIZZA'),
(167, 17, 'Quattro Formaggi', 'Pizza z 4 serami', 29.99, TRUE, 'PIZZA'),
(168, 17, 'Vegetariana', 'Pizza z warzywami', 24.99, TRUE, 'PIZZA'),
(169, 17, 'Calzone', 'Zapiekana pizza', 27.99, TRUE, 'PIZZA');

-- RZESZÓW - 2 restauracje
INSERT INTO users (id, email, password, full_name, role, created_at, updated_at) VALUES
(35, 'admin@rzeszow1.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Admin Rzeszów 1', 'RESTAURANT_ADMIN', NOW(), NOW()),
(36, 'admin@rzeszow2.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Admin Rzeszów 2', 'RESTAURANT_ADMIN', NOW(), NOW());

INSERT INTO restaurants (id, name, address, delivery_price, admin_id, image_url) VALUES
(18, 'Pizza Express Rzeszów', 'ul. 3 Maja 10, Rzeszów', 5.00, 35, '/images/restaurant3.jpg'),
(19, 'Burger House Rzeszów', 'ul. Grunwaldzka 15, Rzeszów', 5.50, 36, '/images/restaurant4.jpg');

INSERT INTO users (id, email, password, full_name, role, restaurant_id, created_at, updated_at) VALUES
(37, 'courier@rzeszow1.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Kurier Rzeszów 1', 'COURIER', 18, NOW(), NOW()),
(38, 'courier@rzeszow2.pl', '{bcrypt}$2a$10$abcdefghijklmnopqrstuv', 'Kurier Rzeszów 2', 'COURIER', 19, NOW(), NOW());

INSERT INTO dishes (id, restaurant_id, name, description, price, available, category) VALUES
-- Pizza Express Rzeszów
(170, 18, 'Margherita', 'Klasyczna pizza z sosem pomidorowym i mozzarellą', 23.99, TRUE, 'PIZZA'),
(171, 18, 'Pepperoni', 'Pizza z pikantnym salami pepperoni', 26.50, TRUE, 'PIZZA'),
-- Burger House Rzeszów
(172, 19, 'Classic Burger', 'Wołowina, ser, sałata, pomidor, sos', 24.00, TRUE, 'BURGER'),
(173, 19, 'Cheeseburger', 'Wołowina, podwójny ser, sos BBQ', 27.00, TRUE, 'BURGER');

-- Zaktualizuj sekwencje do najwyższego użytego ID + 1
-- To zapewnia, że nowe rekordy będą miały unikalne ID
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('restaurants_id_seq', (SELECT MAX(id) FROM restaurants));
SELECT setval('dishes_id_seq', (SELECT MAX(id) FROM dishes));
SELECT setval('addresses_id_seq', COALESCE((SELECT MAX(id) FROM addresses), 1));
SELECT setval('orders_id_seq', COALESCE((SELECT MAX(id) FROM orders), 1));
SELECT setval('order_items_id_seq', COALESCE((SELECT MAX(id) FROM order_items), 1));
SELECT setval('reviews_id_seq', COALESCE((SELECT MAX(id) FROM reviews), 1));

