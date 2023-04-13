INSERT INTO test_user (id, username, password) VALUES (1, 'admin', 'admin');
INSERT INTO test_user (id, username, password) VALUES (2, 'user','user');
INSERT INTO test_user (id, username, password) VALUES (3, 'noRoleUser','noRoleUser');

INSERT INTO test_role (id, role_name) VALUES (1, 'admin');
INSERT INTO test_role (id, role_name) VALUES (2, 'user');

INSERT INTO test_user_role (user_id, role_id) VALUES (1, 1);
INSERT INTO test_user_role (user_id, role_id) VALUES (2, 2);
