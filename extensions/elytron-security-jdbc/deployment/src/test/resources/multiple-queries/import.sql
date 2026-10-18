DROP TABLE test_user IF EXISTS;

CREATE TABLE test_user (
  id INT,
  username VARCHAR(255),
  password VARCHAR(255)
);

DROP TABLE test_role IF EXISTS;

CREATE TABLE test_role (
  id INT,
  role_name VARCHAR(255)
);

DROP TABLE test_user_role IF EXISTS;

CREATE TABLE test_user_role (
  user_id INT,
  role_id INT
);

INSERT INTO test_user (id, username, password) VALUES (1, 'admin', 'admin');
INSERT INTO test_user (id, username, password) VALUES (2, 'user','user');
INSERT INTO test_user (id, username, password) VALUES (3, 'noRoleUser','noRoleUser');

INSERT INTO test_role (id, role_name) VALUES (1, 'admin');
INSERT INTO test_role (id, role_name) VALUES (2, 'user');

INSERT INTO test_user_role (user_id, role_id) VALUES (1, 1);
INSERT INTO test_user_role (user_id, role_id) VALUES (2, 2);
