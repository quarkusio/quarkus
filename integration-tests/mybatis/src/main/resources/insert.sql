CREATE TABLE USERS (
    id integer not null primary key,
    name varchar(80) not null
);

DELETE FROM users;
insert into users (id, name) values(1, 'Test User1');
insert into users (id, name) values(2, 'Test User2');
insert into users (id, name) values(3, 'Test User3');
