CREATE TABLE TEST_SCHEMA.quarkus
(
    id   INT,
    name VARCHAR(20)
);
INSERT INTO TEST_SCHEMA.quarkus(id, name)
VALUES (1, '#[foo]');