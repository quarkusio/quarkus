CREATE USER 'user2'@'%' IDENTIFIED BY 'user2';
GRANT ALL PRIVILEGES ON hibernate_orm_test.* TO 'user2'@'%';
FLUSH PRIVILEGES;