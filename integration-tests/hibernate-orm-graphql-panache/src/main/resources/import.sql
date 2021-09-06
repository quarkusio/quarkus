insert into author(id, name, dob) values (nextval('hibernate_sequence'), 'Fyodor Dostoevsky', '1821-11-11');

insert into book(id, title, author_id) values (nextval('hibernate_sequence'), 'Crime and Punishment', 1);
insert into book(id, title, author_id) values (nextval('hibernate_sequence'), 'Idiot', 1);
insert into book(id, title, author_id) values (nextval('hibernate_sequence'), 'Demons', 1);
insert into book(id, title, author_id) values (nextval('hibernate_sequence'), 'The adolescent', 1);
