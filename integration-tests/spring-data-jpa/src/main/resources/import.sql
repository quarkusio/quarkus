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

INSERT INTO movie(id, title, rating, duration, version) VALUES(1, 'Godzilla: King of the Monsters', 'PG-13', 132, 1);
INSERT INTO movie(id, title, rating, duration, version) VALUES(2, 'Avengers: Endgame', 'PG-13', 181, 1);
INSERT INTO movie(id, title, rating, duration, version) VALUES(3, 'Interstellar', 'PG-13', 169, 1);
INSERT INTO movie(id, title, rating, duration, version) VALUES(4, 'Aladdin', 'PG', 128, 1);
INSERT INTO movie(id, title, rating, duration, version) VALUES(5, 'Die Hard', 'R', 132, 1);
INSERT INTO movie(id, title, rating, duration, version) VALUES(6, 'The Departed', 'R', 151, 1);
INSERT INTO movie(id, title, rating, duration, version) VALUES(7, 'Dunkirk', null, 146, 1);
INSERT INTO movie(id, title, rating, duration, version) VALUES(8, 'Toy Story 4', 'G', 100, 1);

INSERT INTO address(id, street_name, street_number, zip_code) VALUES (1, 'Easy Street', '10000', '123456');
INSERT INTO address(id, street_name, street_number, zip_code) VALUES (2, 'Blockbuster Avenue', '1', '654321');

INSERT INTO person(id, name, age, joined, active, address_id) VALUES (1, 'Bob', 43, '2018-01-01', true, 1);
INSERT INTO person(id, name, age, joined, active, address_id) VALUES (2, 'Florence', 41, '2018-02-02', true, 2);
INSERT INTO person(id, name, age, joined, active, address_id) VALUES (3, 'DeMar', 28, '2017-03-03', false, 1);
INSERT INTO person(id, name, age, joined, active, address_id) VALUES (4, 'DeMar', 55, '2010-04-04', false, NULL);
INSERT INTO person(id, name, age, joined, active, address_id) VALUES (5, 'DeMar', 20, '2019-05-05', true, NULL);
INSERT INTO person(id, name, age, joined, active, address_id) VALUES (6, null , 22, '2019-06-06', true, NULL);

INSERT INTO post(id, title, bypass, posted, organization) VALUES (1, 'Quarkus first public release!', false, '2019-03-01 12:00:00.000', 'RH');
INSERT INTO post(id, title, bypass, posted, organization) VALUES (2, 'Quarkus 0.12.0 released', false, '2019-04-01 12:00:00.000', 'RH');
INSERT INTO post(id, title, bypass, posted, organization) VALUES (3, 'Quarkus 0.20 released', false, '2019-06-01 12:00:00.000', 'RH');

INSERT INTO post_comment(id, post_id, review) VALUES (1, 1, 'Excellent!');
INSERT INTO post_comment(id, post_id, review) VALUES (2, 1, 'Wonderful!');

INSERT INTO song(id, title, author) VALUES (1, 'Consejo de sabios' , 'Vetusta Morla');
INSERT INTO song(id, title, author) VALUES (2, 'Nothing else mothers' , 'Metallica');
INSERT INTO song(id, title, author) VALUES (3, 'Ephedra' , 'My Sleeping Karma');
INSERT INTO song(id, title, author) VALUES (4, 'Whatever it takes' , 'Imagine Dragons');
INSERT INTO song(id, title, author) VALUES (5, 'Santos que yo te pinte' , 'Los planetas');
INSERT INTO song(id, title, author) VALUES (6, 'Drinkee' , 'Sofi Tukker');

INSERT INTO liked_songs(person_id, song_id) VALUES (1,1);
INSERT INTO liked_songs(person_id, song_id) VALUES (1,3);
INSERT INTO liked_songs(person_id, song_id) VALUES (1,4);
INSERT INTO liked_songs(person_id, song_id) VALUES (2,5);
INSERT INTO liked_songs(person_id, song_id) VALUES (2,6);
INSERT INTO liked_songs(person_id, song_id) VALUES (3,1);
INSERT INTO liked_songs(person_id, song_id) VALUES (4,3);
INSERT INTO liked_songs(person_id, song_id) VALUES (4,4);
INSERT INTO liked_songs(person_id, song_id) VALUES (5,5);
INSERT INTO liked_songs(person_id, song_id) VALUES (6,6);

INSERT INTO customer(id, first_name, last_name, email, telephone, enabled) VALUES (1, 'Jason', 'Bourne',  'jason.bourne@mail.com', '0102030405', TRUE);
INSERT INTO customer(id, first_name, last_name, email, telephone, enabled) VALUES (2, 'Homer', 'Simpson', 'homer.simpson@mail.com',  '0605040302', TRUE);
INSERT INTO customer(id, first_name, last_name, email, telephone, enabled) VALUES (3, 'Peter', 'Quin', 'pater.quin@mail.com',  '0706050403', FALSE);

INSERT INTO cart(id, customer_id, status) VALUES (1, 1, 'NEW');
INSERT INTO cart(id, customer_id, status) VALUES (2, 2, 'NEW');
INSERT INTO cart(id, customer_id, status) VALUES (3, 3, 'CANCELED');

INSERT INTO orders(id, cart_id) VALUES (1, 1);
INSERT INTO orders(id, cart_id) VALUES (2, 2);

INSERT INTO unit(id, name) VALUES (1, 'Delivery Unit');
INSERT INTO unit(id, name) VALUES (2, 'Sales and Marketing Unit');
INSERT INTO team(id, name, unit_id) VALUES (10, 'Development Team', 1);
INSERT INTO team(id, name, unit_id) VALUES (11, 'Sales Team', 2);
INSERT INTO employee(id, user_id, first_name, last_name, team_id) VALUES (100, 'johdoe', 'John', 'Doe', 10);
INSERT INTO employee(id, user_id, first_name, last_name, team_id) VALUES (101, 'petdig', 'Peter', 'Digger', 10);
INSERT INTO employee(id, user_id, first_name, last_name, team_id) VALUES (102, 'stesmi', 'Stella', 'Smith', 11);

INSERT INTO MotorCar(id, brand, model) VALUES (1, 'Monteverdi', 'Hai 450');
INSERT INTO MotorCar(id, brand, model) VALUES (2, 'Rinspeed', 'iChange');
INSERT INTO MotorCar(id, brand, model) VALUES (3, 'Rinspeed', 'Oasis');

INSERT INTO CatalogValue(id, key, displayName, type) VALUES (1, 'DE-BY', 'Bavaria', 'federalState');
INSERT INTO CatalogValue(id, key, displayName, type) VALUES (2, 'DE-SN', 'Saxony', 'federalState');
INSERT INTO CatalogValue(id, key, displayName, type) VALUES (3, 'DE', 'Germany', 'country');
INSERT INTO CatalogValue(id, key, displayName, type) VALUES (4, 'FR', 'France', 'country');