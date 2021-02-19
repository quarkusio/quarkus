insert into collection(id, name, age) values ('empty', 'empty collection', 13);
insert into collection(id, name, age) values ('full', 'full collection', 13);

insert into item(id, name, collection_id) values (nextval('hibernate_sequence'), 'first', 'full');
insert into item(id, name, collection_id) values (nextval('hibernate_sequence'), 'second', 'full');