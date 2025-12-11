#!/bin/bash
set -e

# Create databases
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE DATABASE orderdb;
    CREATE DATABASE inventorydb;
EOSQL

# Create users and grant privileges for Order DB
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname=orderdb <<-EOSQL
    CREATE USER orderuser WITH PASSWORD 'orderpass';
    GRANT ALL PRIVILEGES ON DATABASE orderdb TO orderuser;
    GRANT ALL ON SCHEMA public TO orderuser;
EOSQL

# Create users and grant privileges for Inventory DB
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname=inventorydb <<-EOSQL
    CREATE USER inventoryuser WITH PASSWORD 'inventorypass';
    GRANT ALL PRIVILEGES ON DATABASE inventorydb TO inventoryuser;
    GRANT ALL ON SCHEMA public TO inventoryuser;
EOSQL

# Create tables for Order Service
psql -v ON_ERROR_STOP=1 --username orderuser --dbname=orderdb <<-EOSQL
    -- Akka Persistence Journal
    CREATE TABLE IF NOT EXISTS journal (
        ordering BIGSERIAL,
        persistence_id VARCHAR(255) NOT NULL,
        sequence_number BIGINT NOT NULL,
        deleted BOOLEAN DEFAULT FALSE,
        tags VARCHAR(255) DEFAULT NULL,
        message BYTEA NOT NULL,
        PRIMARY KEY(persistence_id, sequence_number)
    );

    CREATE UNIQUE INDEX journal_ordering_idx ON journal(ordering);

    -- Akka Persistence Snapshot
    CREATE TABLE IF NOT EXISTS snapshot (
        persistence_id VARCHAR(255) NOT NULL,
        sequence_number BIGINT NOT NULL,
        created BIGINT NOT NULL,
        snapshot BYTEA NOT NULL,
        PRIMARY KEY(persistence_id, sequence_number)
    );

    -- Akka Projection Offset Store
    CREATE TABLE IF NOT EXISTS akka_projection_offset_store (
        projection_name VARCHAR(255) NOT NULL,
        projection_key VARCHAR(255) NOT NULL,
        current_offset VARCHAR(255) NOT NULL,
        manifest VARCHAR(4) NOT NULL,
        mergeable BOOLEAN NOT NULL,
        last_updated BIGINT NOT NULL,
        PRIMARY KEY(projection_name, projection_key)
    );

    -- Order Read Model
    CREATE TABLE IF NOT EXISTS orders (
        order_id VARCHAR(255) PRIMARY KEY,
        customer_id VARCHAR(255) NOT NULL,
        status VARCHAR(50) NOT NULL,
        total_amount DECIMAL(10, 2) NOT NULL,
        created_at TIMESTAMP NOT NULL,
        updated_at TIMESTAMP NOT NULL
    );

    CREATE INDEX orders_customer_id_idx ON orders(customer_id);
    CREATE INDEX orders_status_idx ON orders(status);

    -- Order Items Read Model
    CREATE TABLE IF NOT EXISTS order_items (
        id SERIAL PRIMARY KEY,
        order_id VARCHAR(255) NOT NULL REFERENCES orders(order_id),
        product_id VARCHAR(255) NOT NULL,
        product_name VARCHAR(255) NOT NULL,
        quantity INTEGER NOT NULL,
        price DECIMAL(10, 2) NOT NULL
    );

    CREATE INDEX order_items_order_id_idx ON order_items(order_id);
EOSQL

# Create tables for Inventory Service
psql -v ON_ERROR_STOP=1 --username inventoryuser --dbname=inventorydb <<-EOSQL
    -- Akka Persistence Journal
    CREATE TABLE IF NOT EXISTS journal (
        ordering BIGSERIAL,
        persistence_id VARCHAR(255) NOT NULL,
        sequence_number BIGINT NOT NULL,
        deleted BOOLEAN DEFAULT FALSE,
        tags VARCHAR(255) DEFAULT NULL,
        message BYTEA NOT NULL,
        PRIMARY KEY(persistence_id, sequence_number)
    );

    CREATE UNIQUE INDEX journal_ordering_idx ON journal(ordering);

    -- Akka Persistence Snapshot
    CREATE TABLE IF NOT EXISTS snapshot (
        persistence_id VARCHAR(255) NOT NULL,
        sequence_number BIGINT NOT NULL,
        created BIGINT NOT NULL,
        snapshot BYTEA NOT NULL,
        PRIMARY KEY(persistence_id, sequence_number)
    );

    -- Inventory Read Model
    CREATE TABLE IF NOT EXISTS inventory (
        product_id VARCHAR(255) PRIMARY KEY,
        available_quantity INTEGER NOT NULL,
        reserved_quantity INTEGER NOT NULL,
        updated_at TIMESTAMP NOT NULL
    );

    -- Inventory Reservations Read Model
    CREATE TABLE IF NOT EXISTS inventory_reservations (
        reservation_id VARCHAR(255) PRIMARY KEY,
        product_id VARCHAR(255) NOT NULL,
        order_id VARCHAR(255) NOT NULL,
        quantity INTEGER NOT NULL,
        created_at TIMESTAMP NOT NULL
    );

    CREATE INDEX inventory_reservations_product_id_idx ON inventory_reservations(product_id);
    CREATE INDEX inventory_reservations_order_id_idx ON inventory_reservations(order_id);
EOSQL

echo "Database initialization completed successfully"
