INSERT INTO book(bid, name, publicationYear) VALUES (1, 'Sapiens' , 2011);
INSERT INTO book(bid, name, publicationYear) VALUES (2, 'Homo Deus' , 2015);
INSERT INTO book(bid, name, publicationYear) VALUES (3, 'Enlightenment Now' , 2018);
INSERT INTO book(bid, name, publicationYear) VALUES (4, 'Factfulness' , 2018);
INSERT INTO book(bid, name, publicationYear) VALUES (5, 'Sleepwalkers' , 2012);
INSERT INTO book(bid, name, publicationYear) VALUES (6, 'The Silk Roads' , 2015);

INSERT INTO cat(id, color, breed, distinctive) VALUES (1, 'Grey', 'Scottish Fold', true);
INSERT INTO cat(id, color, breed, distinctive) VALUES (2, 'Grey', 'Persian', true);
INSERT INTO cat(id, color, breed, distinctive) VALUES (3, 'White', 'Turkish Angora', true);
INSERT INTO cat(id, color, breed, distinctive) VALUES (4, null , 'British Shorthair', false );
INSERT INTO cat(id, color, breed, distinctive) VALUES (5, 'Black' , 'Bombay Cat', true);

INSERT INTO country(id, name, iso3) VALUES (1, 'Greece' , 'GRC');
INSERT INTO country(id, name, iso3) VALUES (2, 'France' , 'FRA');
INSERT INTO country(id, name, iso3) VALUES (3, 'Czechia' , 'CZE');

INSERT INTO movie(id, title, rating, duration) VALUES(1, 'Godzilla: King of the Monsters', 'PG-13', 132);
INSERT INTO movie(id, title, rating, duration) VALUES(2, 'Avengers: Endgame', 'PG-13', 181);
INSERT INTO movie(id, title, rating, duration) VALUES(3, 'Interstellar', 'PG-13', 169);
INSERT INTO movie(id, title, rating, duration) VALUES(4, 'Aladdin', 'PG', 128);
INSERT INTO movie(id, title, rating, duration) VALUES(5, 'Die Hard', 'R', 132);
INSERT INTO movie(id, title, rating, duration) VALUES(6, 'The Departed', 'R', 151);
INSERT INTO movie(id, title, rating, duration) VALUES(7, 'Dunkirk', null, 146);
INSERT INTO movie(id, title, rating, duration) VALUES(8, 'Toy Story 4', 'G', 100);

INSERT INTO address(id, street_name, street_number, zip_code) VALUES (1, 'Easy Street', '10000', '123456');
INSERT INTO address(id, street_name, street_number, zip_code) VALUES (2, 'Blockbuster Avenue', '1', '654321');

INSERT INTO person(id, name, age, joined, active, address_id) VALUES (1, 'Bob', 43, '2018-01-01', true, 1);
INSERT INTO person(id, name, age, joined, active, address_id) VALUES (2, 'Florence', 41, '2018-02-02', true, 2);
INSERT INTO person(id, name, age, joined, active, address_id) VALUES (3, 'DeMar', 28, '2017-03-03', false, 1);
INSERT INTO person(id, name, age, joined, active, address_id) VALUES (4, 'DeMar', 55, '2010-04-04', false, NULL);
INSERT INTO person(id, name, age, joined, active, address_id) VALUES (5, 'DeMar', 20, '2019-05-05', true, NULL);
INSERT INTO person(id, name, age, joined, active, address_id) VALUES (6, null , 22, '2019-06-06', true, NULL);

INSERT INTO post(id, title, bypass) VALUES (1, 'Quarkus first public release!', false);
INSERT INTO post(id, title, bypass) VALUES (2, 'Quarkus 0.12.0 released', false);
INSERT INTO post(id, title, bypass) VALUES (3, 'Quarkus 0.20 released', false);

INSERT INTO post_comment(id, post_id, review) VALUES (1, 1, 'Excellent!');
INSERT INTO post_comment(id, post_id, review) VALUES (2, 1, 'Wonderful!');

