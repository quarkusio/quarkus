CREATE TABLE known_fruits
(
  tenantId  VARCHAR(40),
  id        INT,
  name      VARCHAR(40)
);
CREATE SEQUENCE known_fruits_id_seq START WITH 7;
INSERT INTO known_fruits(tenantId, id, name) VALUES ('base', 1, 'Cherry');
INSERT INTO known_fruits(tenantId, id, name) VALUES ('base', 2, 'Apple');
INSERT INTO known_fruits(tenantId, id, name) VALUES ('base', 3, 'Banana');

INSERT INTO known_fruits(tenantId, id, name) VALUES ('mycompany', 4, 'Avocado');
INSERT INTO known_fruits(tenantId, id, name) VALUES ('mycompany', 5, 'Apricots');
INSERT INTO known_fruits(tenantId, id, name) VALUES ('mycompany', 6, 'Blackberries');

