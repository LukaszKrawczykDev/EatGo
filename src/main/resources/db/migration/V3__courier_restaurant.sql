ALTER TABLE users
    ADD COLUMN IF NOT EXISTS restaurant_id BIGINT REFERENCES restaurants(id);


