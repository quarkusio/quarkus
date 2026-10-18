CREATE TABLE TEST_SCHEMA.quarkus
(
    id   INT,
    name VARCHAR(20),
    createdBy VARCHAR(100) DEFAULT CURRENT_USER NOT NULL
);
INSERT INTO TEST_SCHEMA.quarkus(id, name) VALUES (1, '#[foo]');