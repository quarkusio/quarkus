insert into author(id, name, dob) values (nextval('hibernate_sequence'), 'Fyodor Dostoevsky', '1821-11-11');
insert into author(id, name, dob) values (nextval('hibernate_sequence'), 'George Orwell', '1903-06-25');

insert into book(id, title, author_id) values (nextval('hibernate_sequence'), 'Crime and Punishment', 1);
insert into book(id, title, author_id) values (nextval('hibernate_sequence'), 'Idiot', 1);
insert into book(id, title, author_id) values (nextval('hibernate_sequence'), 'The Brothers Karamazov', 1);

insert into review(id, text, book_id) values('first', 'Captivating', 3);
insert into review(id, text, book_id) values('second', 'Amazing', 3);