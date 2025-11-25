-- Dodaj pola ustawień użytkownika
ALTER TABLE users 
ADD COLUMN default_city VARCHAR(100),
ADD COLUMN default_address_id BIGINT,
ADD COLUMN theme VARCHAR(10) DEFAULT 'light';

-- Dodaj klucz obcy dla domyślnego adresu
ALTER TABLE users
ADD CONSTRAINT fk_user_default_address
FOREIGN KEY (default_address_id) REFERENCES addresses(id) ON DELETE SET NULL;

-- Ustaw domyślny motyw dla istniejących użytkowników
UPDATE users SET theme = 'light' WHERE theme IS NULL;

