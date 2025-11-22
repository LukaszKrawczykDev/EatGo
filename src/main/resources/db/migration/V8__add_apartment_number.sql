ALTER TABLE addresses
    ADD COLUMN IF NOT EXISTS apartment_number VARCHAR(20);

