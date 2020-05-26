insert into collection(name) values ('empty');
insert into collection(name) values ('full');

insert into item(id, name, collection_name) values (nextval('hibernate_sequence'), 'first', 'full');
insert into item(id, name, collection_name) values (nextval('hibernate_sequence'), 'second', 'full');