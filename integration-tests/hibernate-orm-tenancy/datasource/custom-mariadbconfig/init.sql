CREATE OR REPLACE DATABASE base;
CREATE OR REPLACE DATABASE mycompany;
CREATE OR REPLACE DATABASE inventory;
CREATE OR REPLACE DATABASE inventorymycompany;

CREATE OR REPLACE USER 'jane'@'%' IDENTIFIED BY 'abc';
GRANT ALL privileges ON base.* TO 'jane'@'%';
GRANT ALL privileges ON inventory.* TO 'jane'@'%';

CREATE OR REPLACE USER 'john'@'%' IDENTIFIED BY 'def';
GRANT ALL privileges ON mycompany.* TO 'john'@'%';
GRANT ALL privileges ON inventorymycompany.* TO 'john'@'%';

FLUSH PRIVILEGES;
