CREATE SCHEMA "base";
CREATE TABLE "base".known_fruits
(
  id   INT,
  name VARCHAR(40)
);
CREATE SEQUENCE "base".known_fruits_id_seq START WITH 1;

CREATE SCHEMA "company1";
CREATE TABLE "company1".known_fruits
(
  id   INT,
  name VARCHAR(40)
);
CREATE SEQUENCE "company1".known_fruits_id_seq START WITH 1;

CREATE SCHEMA "company2";
CREATE TABLE "company2".known_fruits
(
  id   INT,
  name VARCHAR(40)
);
CREATE SEQUENCE "company2".known_fruits_id_seq START WITH 1;
