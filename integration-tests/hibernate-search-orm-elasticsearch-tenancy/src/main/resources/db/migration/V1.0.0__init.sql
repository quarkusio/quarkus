DROP SCHEMA IF EXISTS "base" CASCADE;
DROP SCHEMA IF EXISTS "company1" CASCADE;
DROP SCHEMA IF EXISTS "company2" CASCADE;
DROP SCHEMA IF EXISTS "company3" CASCADE;
DROP SCHEMA IF EXISTS "company4" CASCADE;

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

-- Books tables:

CREATE TABLE "base".books
(
    id   INT,
    name VARCHAR(40)
);
CREATE SEQUENCE "base".books_id_seq START WITH 1;

CREATE SCHEMA "company3";
CREATE TABLE "company3".books
(
    id   INT,
    name VARCHAR(40)
);
CREATE SEQUENCE "company3".books_id_seq START WITH 1;

CREATE SCHEMA "company4";
CREATE TABLE "company4".books
(
    id   INT,
    name VARCHAR(40)
);
CREATE SEQUENCE "company4".books_id_seq START WITH 1;
