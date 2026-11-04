INSERT INTO MyEntity(id, name) VALUES(1, 'default schema init script entity');
INSERT INTO MyEntity(id, name) VALUES(2, 'schema.sql schema init script entity');
ALTER SEQUENCE myEntitySeq RESTART WITH 3;
