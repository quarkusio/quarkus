CREATE OR REPLACE DATABASE base;
CREATE OR REPLACE DATABASE mycompany;
CREATE OR REPLACE DATABASE inventory;
CREATE OR REPLACE DATABASE inventorymycompany;

CREATE OR REPLACE USER 'jane'@'%' IDENTIFIED BY 'abc';
GRANT ALL privileges ON base.* TO 'jane'@'%';
GRANT ALL privileges ON mycompany.* TO 'jane'@'%';

CREATE OR REPLACE USER 'john'@'%' IDENTIFIED BY 'def';
GRANT ALL privileges ON inventory.* TO 'john'@'%';
GRANT ALL privileges ON inventorymycompany.* TO 'john'@'%';

FLUSH PRIVILEGES;

USE base;
CREATE TABLE known_fruits
(
  id   INT,
  name VARCHAR(40)
);
CREATE SEQUENCE known_fruits_id_seq START WITH 4;
INSERT INTO known_fruits(id, name) VALUES (1, 'Cherry');
INSERT INTO known_fruits(id, name) VALUES (2, 'Apple');
INSERT INTO known_fruits(id, name) VALUES (3, 'Banana');

USE inventory;
CREATE TABLE plane
(
  id   INT,
  name VARCHAR(255)
);
CREATE SEQUENCE plane_id_seq START WITH 3;
INSERT INTO plane(id, name) VALUES (1, 'Airbus A320');
INSERT INTO plane(id, name) VALUES (2, 'Airbus A350');

USE inventorymycompany;
CREATE TABLE plane
(
  id   INT,
  name VARCHAR(40)
);
CREATE SEQUENCE plane_iq_seq START WITH 3;
INSERT INTO plane(id, name) VALUES (1, 'Boeing 737');
INSERT INTO plane(id, name) VALUES (2, 'Boeing 747');

USE mycompany;
CREATE TABLE known_fruits
(
  id   INT,
  name VARCHAR(40)
);
CREATE SEQUENCE known_fruits_id_seq START WITH 4;
INSERT INTO known_fruits(id, name) VALUES (1, 'Avocado');
INSERT INTO known_fruits(id, name) VALUES (2, 'Apricots');
INSERT INTO known_fruits(id, name) VALUES (3, 'Blackberries');
