insert into collection(id, name) values ('empty', 'empty collection');
insert into collection(id, name) values ('full', 'full collection');

insert into item(id, name, collection_id) values (1, 'first', 'full');
insert into item(id, name, collection_id) values (2, 'second', 'full');
-- Tell ORM we used 2 items
alter sequence item_seq restart with 3;

-- do not add elements to emptylistitem, it should be kept empty
