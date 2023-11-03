INSERT INTO test_user (id, username, password) VALUES (1, 'admin', 'admin');
INSERT INTO test_user (id, username, password) VALUES (2, 'user','user');
INSERT INTO test_user (id, username, password) VALUES (3, 'noRoleUser','noRoleUser');

INSERT INTO test_role (user_id, role_name) VALUES (1, 'admin');
INSERT INTO test_role (user_id, role_name) VALUES (2, 'user');
