DROP TABLE test_role IF EXISTS;

CREATE TABLE test_role (
  id INT,
  role_name VARCHAR(255)
);

DROP TABLE test_user_role IF EXISTS;

CREATE TABLE test_user_role (
  username VARCHAR(255),
  role_id INT
);

INSERT INTO test_role (id, role_name) VALUES (1, 'admin');
INSERT INTO test_role (id, role_name) VALUES (2, 'user');

INSERT INTO test_user_role (username, role_id) VALUES ('admin', 1);
INSERT INTO test_user_role (username, role_id) VALUES ('user', 2);
