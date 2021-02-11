-- We need an existing table in the database to test the creation of the
-- Liquibase history table with the correct baseline version set.
CREATE TABLE IF NOT EXISTS PUBLIC.fake_existing_tbl;