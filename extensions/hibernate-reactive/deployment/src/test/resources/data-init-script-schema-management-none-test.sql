CREATE TABLE IF NOT EXISTS Hero_for_DataInitScriptNoneTest(id bigint PRIMARY KEY, name varchar(255) UNIQUE);
INSERT INTO Hero_for_DataInitScriptNoneTest(id, name) VALUES (1, 'Galadriel') ON CONFLICT DO NOTHING;
