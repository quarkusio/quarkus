CREATE TABLE multiple_flyway_test
(
    id   INT,
    name VARCHAR(255)
);
INSERT INTO multiple_flyway_test(id, name)
VALUES (1, 'Multiple flyway datasources should work seamlessly in JVM and native mode');