CREATE SCHEMA "base";
CREATE TABLE "base".known_fruits
(
  id   INT,
  name VARCHAR(40)
);
CREATE SEQUENCE "base".known_fruits_id_seq START WITH 4;
INSERT INTO "base".known_fruits(id, name) VALUES (1, 'Cherry');
INSERT INTO "base".known_fruits(id, name) VALUES (2, 'Apple');
INSERT INTO "base".known_fruits(id, name) VALUES (3, 'Banana');