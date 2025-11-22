-- Dodaj kolumnę image_url do dań
ALTER TABLE dishes
    ADD COLUMN IF NOT EXISTS image_url VARCHAR(255);

