insert into collection(id, name) values ('empty', 'empty collection');
insert into collection(id, name) values ('full', 'full collection');

insert into item(id, name, collection_id) values (nextval('hibernate_sequence'), 'first', 'full');
insert into item(id, name, collection_id) values (nextval('hibernate_sequence'), 'second', 'full');

-- do not add elements to emptylistitem, it should be kept empty