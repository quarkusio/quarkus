DROP TABLE test_user IF EXISTS;

CREATE TABLE test_user (
  id INT,
  username VARCHAR(255),
  password VARCHAR(255),
  roles VARCHAR(255)
);

INSERT INTO test_user (id, username, password, roles) VALUES (1, 'admin', 'admin', 'admin, user');
INSERT INTO test_user (id, username, password, roles) VALUES (2, 'user','user', 'user,tester');
INSERT INTO test_user (id, username, password, roles) VALUES (3, 'noRoleUser','noRoleUser', '');
