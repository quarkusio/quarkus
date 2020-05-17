CREATE TABLE known_fruits
(
  id   INT,
  name VARCHAR(40)
);
CREATE SEQUENCE known_fruits_id_seq START WITH 4;
INSERT INTO known_fruits(id, name) VALUES (1, 'Cherry');
INSERT INTO known_fruits(id, name) VALUES (2, 'Apple');
INSERT INTO known_fruits(id, name) VALUES (3, 'Banana');
