CREATE TABLE TEST_SCHEMA.quarkus_table2
(
    id   INT,
    name VARCHAR(20),
    createdBy VARCHAR(100) DEFAULT CURRENT_USER NOT NULL
);
INSERT INTO TEST_SCHEMA.quarkus_table2(id, name)
VALUES (1, '1.0.1 #[title]');
