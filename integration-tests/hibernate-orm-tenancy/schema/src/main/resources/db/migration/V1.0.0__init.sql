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

CREATE SCHEMA "mycompany";
CREATE TABLE "mycompany".known_fruits
(
  id   INT,
  name VARCHAR(40)
);
CREATE SEQUENCE "mycompany".known_fruits_id_seq START WITH 4;
INSERT INTO "mycompany".known_fruits(id, name) VALUES (1, 'Avocado');
INSERT INTO "mycompany".known_fruits(id, name) VALUES (2, 'Apricots');
INSERT INTO "mycompany".known_fruits(id, name) VALUES (3, 'Blackberries');

CREATE SCHEMA "inventory";
CREATE TABLE "inventory".plane
(
  id   INT,
  name VARCHAR(255)
);
CREATE SEQUENCE "inventory".plane_id_seq START WITH 3;
INSERT INTO "inventory".plane(id, name) VALUES (1, 'Airbus A320');
INSERT INTO "inventory".plane(id, name) VALUES (2, 'Airbus A350');

CREATE SCHEMA "inventorymycompany";
CREATE TABLE "inventorymycompany".plane
(
  id   INT,
  name VARCHAR(40)
);
CREATE SEQUENCE "inventorymycompany".plane_iq_seq START WITH 3;
INSERT INTO "inventorymycompany".plane(id, name) VALUES (1, 'Boeing 737');
INSERT INTO "inventorymycompany".plane(id, name) VALUES (2, 'Boeing 747');
