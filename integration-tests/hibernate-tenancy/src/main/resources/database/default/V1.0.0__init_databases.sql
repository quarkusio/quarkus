CREATE DATABASE base;
CREATE USER 'jane'@'%' IDENTIFIED BY 'abc';
GRANT ALL privileges ON base.* TO 'jane'@'%';

CREATE DATABASE mycompany;
CREATE USER 'john'@'%' IDENTIFIED BY 'def';
GRANT ALL privileges ON mycompany.* TO 'john'@'%';

FLUSH PRIVILEGES;
