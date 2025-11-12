CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       full_name VARCHAR(255) NOT NULL,
                       role VARCHAR(50) NOT NULL,
                       restaurant_id BIGINT,
                       created_at TIMESTAMP DEFAULT NOW(),
                       updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE restaurants (
                             id BIGSERIAL PRIMARY KEY,
                             name VARCHAR(255) NOT NULL,
                             address VARCHAR(255) NOT NULL,
                             delivery_price NUMERIC(10,2) NOT NULL DEFAULT 0,
                             admin_id BIGINT NOT NULL,
                             FOREIGN KEY (admin_id) REFERENCES users(id)
);

ALTER TABLE users
    ADD CONSTRAINT fk_users_restaurant
        FOREIGN KEY (restaurant_id) REFERENCES restaurants(id);

CREATE TABLE dishes (
                        id BIGSERIAL PRIMARY KEY,
                        restaurant_id BIGINT NOT NULL,
                        name VARCHAR(255) NOT NULL,
                        description TEXT,
                        price NUMERIC(10,2) NOT NULL CHECK (price >= 0),
                        available BOOLEAN DEFAULT TRUE,
                        FOREIGN KEY (restaurant_id) REFERENCES restaurants(id)
);

CREATE TABLE addresses (
                           id BIGSERIAL PRIMARY KEY,
                           user_id BIGINT NOT NULL,
                           city VARCHAR(100) NOT NULL,
                           street VARCHAR(255) NOT NULL,
                           postal_code VARCHAR(20) NOT NULL,
                           FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE orders (
                        id BIGSERIAL PRIMARY KEY,
                        user_id BIGINT NOT NULL,
                        restaurant_id BIGINT NOT NULL,
                        courier_id BIGINT,
                        address_id BIGINT NOT NULL,
                        status VARCHAR(50) NOT NULL DEFAULT 'PLACED',
                        total_price NUMERIC(10,2) NOT NULL CHECK (total_price >= 0),
                        delivery_price NUMERIC(10,2) NOT NULL DEFAULT 0,
                        created_at TIMESTAMP DEFAULT NOW(),
                        updated_at TIMESTAMP DEFAULT NOW(),
                        FOREIGN KEY (user_id) REFERENCES users(id),
                        FOREIGN KEY (restaurant_id) REFERENCES restaurants(id),
                        FOREIGN KEY (courier_id) REFERENCES users(id),
                        FOREIGN KEY (address_id) REFERENCES addresses(id)
);

CREATE TABLE order_items (
                             id BIGSERIAL PRIMARY KEY,
                             order_id BIGINT NOT NULL,
                             dish_id BIGINT NOT NULL,
                             quantity INT NOT NULL CHECK (quantity > 0),
                             price_snapshot NUMERIC(10,2) NOT NULL,
                             FOREIGN KEY (order_id) REFERENCES orders(id),
                             FOREIGN KEY (dish_id) REFERENCES dishes(id)
);

CREATE TABLE reviews (
                         id BIGSERIAL PRIMARY KEY,
                         order_id BIGINT NOT NULL,
                         reviewer_id BIGINT NOT NULL,
                         target_type VARCHAR(50) NOT NULL, -- RESTAURANT / COURIER
                         target_id BIGINT NOT NULL,
                         rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
                         comment TEXT,
                         created_at TIMESTAMP DEFAULT NOW(),
                         FOREIGN KEY (order_id) REFERENCES orders(id),
                         FOREIGN KEY (reviewer_id) REFERENCES users(id)
);