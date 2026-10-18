DROP TABLE test_user IF EXISTS;

CREATE TABLE test_user (
  id INT,
  username VARCHAR(255),
  password_hash VARCHAR(255),
  salt VARCHAR(255),
  iteration_count INT,
  role VARCHAR(255)
);

INSERT INTO test_user (id, username, password_hash, salt, iteration_count, role) VALUES (1, 'admin', 'zWhRw/6/wzugNHhRZVch18/nSrqzHVw=', '8KC0SE2MSbRZqttat0umqA==', 10, 'admin');
INSERT INTO test_user (id, username, password_hash, salt, iteration_count, role) VALUES (2, 'user','kUHrG6U7M7GzeoUUbE7i/sohIQOLKVY=', 'Sp8vZs4uVC5tiyG53TIwhA==', 10, 'user');
INSERT INTO test_user (id, username, password_hash, salt, iteration_count, role) VALUES (3, 'noRoleUser', 'jO2XHk6BFUvi6NulDSDIxrFZgbqYqYY=', 'TdEVDZtdnaBZzOAg6UBZlg==', 10, '');
