ALTER TABLE users
ADD COLUMN default_city VARCHAR(100),
ADD COLUMN default_address_id BIGINT,
ADD COLUMN theme VARCHAR(10) DEFAULT 'light';

ALTER TABLE users
ADD CONSTRAINT fk_user_default_address
FOREIGN KEY (default_address_id) REFERENCES addresses(id) ON DELETE SET NULL;

UPDATE users SET theme = 'light' WHERE theme IS NULL;

