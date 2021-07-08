CREATE DATABASE IF NOT EXISTS base;
CREATE USER IF NOT EXISTS 'jane'@'%' IDENTIFIED BY 'abc';
GRANT ALL privileges ON base.* TO 'jane'@'%';

CREATE DATABASE IF NOT EXISTS mycompany;
CREATE USER IF NOT EXISTS 'john'@'%' IDENTIFIED BY 'def';
GRANT ALL privileges ON mycompany.* TO 'john'@'%';

CREATE DATABASE IF NOT EXISTS inventory;
CREATE USER IF NOT EXISTS 'jane'@'%' IDENTIFIED BY 'abc';
GRANT ALL privileges ON inventory.* TO 'jane'@'%';

CREATE DATABASE IF NOT EXISTS inventorymycompany;
CREATE USER IF NOT EXISTS 'john'@'%' IDENTIFIED BY 'def';
GRANT ALL privileges ON inventorymycompany.* TO 'john'@'%';

FLUSH PRIVILEGES;
