CREATE TABLE Hero_for_DataInitScriptNoneTest(id bigint PRIMARY KEY, name varchar(255) UNIQUE, created_by_script boolean NOT NULL DEFAULT true);
INSERT INTO Hero_for_DataInitScriptNoneTest(id, name) VALUES (1, 'Galadriel');
