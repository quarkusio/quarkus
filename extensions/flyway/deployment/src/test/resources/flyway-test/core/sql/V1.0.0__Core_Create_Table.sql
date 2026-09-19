CREATE TABLE core_operations (
    id INT PRIMARY KEY,
    operation_name VARCHAR(100),
    operation_status VARCHAR(50)
);

INSERT INTO core_operations (id, operation_name, operation_status) VALUES (1, 'core_op_1', 'active');
INSERT INTO core_operations (id, operation_name, operation_status) VALUES (2, 'core_op_2', 'completed');
