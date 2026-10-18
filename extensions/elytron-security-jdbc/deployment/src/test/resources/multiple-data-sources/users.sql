DROP TABLE test_user IF EXISTS;

CREATE TABLE test_user (
  id INT,
  username VARCHAR(255),
  password VARCHAR(255)
);

INSERT INTO test_user (id, username, password) VALUES (1, 'admin', 'admin');
INSERT INTO test_user (id, username, password) VALUES (2, 'user','user');
INSERT INTO test_user (id, username, password) VALUES (3, 'noRoleUser','noRoleUser');
