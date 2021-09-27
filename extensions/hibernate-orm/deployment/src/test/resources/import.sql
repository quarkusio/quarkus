INSERT INTO MyEntity(id, name) VALUES(1, 'default sql load script entity');
INSERT INTO MyEntity(id, name) VALUES(2, 'import.sql load script entity');
alter sequence myEntitySeq restart with 3;