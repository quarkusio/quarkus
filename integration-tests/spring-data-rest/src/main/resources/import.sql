insert into author(id, name, dob) values (1, 'Fyodor Dostoevsky', '1821-11-11');
alter sequence Author_SEQ restart with 2;

insert into book(id, title, author_id) values (1, 'Crime and Punishment', 1);
insert into book(id, title, author_id) values (2, 'Idiot', 1);
alter sequence Book_SEQ restart with 3;
