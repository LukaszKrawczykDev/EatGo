-- Dodaj kategorię do dań
ALTER TABLE dishes
    ADD COLUMN IF NOT EXISTS category VARCHAR(50);

-- Dodaj obraz do restauracji
ALTER TABLE restaurants
    ADD COLUMN IF NOT EXISTS image_url VARCHAR(255);
