DROP TABLE test_user IF EXISTS;

CREATE TABLE test_user (
  id INT,
  username VARCHAR(255),
  password VARCHAR(255),
  role VARCHAR(255)
);

INSERT INTO test_user (id, username, password, role) VALUES (1, 'admin', 'admin', 'admin');
INSERT INTO test_user (id, username, password, role) VALUES (2, 'user','user', 'user');
INSERT INTO test_user (id, username, password, role) VALUES (3, 'noRoleUser','noRoleUser', '');
