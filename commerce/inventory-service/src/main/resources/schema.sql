CREATE TABLE IF NOT EXISTS inventory (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id BIGINT NOT NULL UNIQUE,
    quantity INTEGER NOT NULL,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    version BIGINT
);

CREATE INDEX IF NOT EXISTS idx_inventory_product_id ON inventory(product_id);