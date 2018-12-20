-- Do not set up hibernate_sequence because JPA does it

DROP TABLE IF EXISTS RxPerson;

CREATE TABLE RxPerson (id integer not null, name varchar, PRIMARY KEY (id));
