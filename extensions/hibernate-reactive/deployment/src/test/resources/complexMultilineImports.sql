-- tag::adocSQL[]
INSERT INTO hero(id, name, otherName, picture, powers, level)
VALUES (1, 'Chewbacca', '', 'https://www.superherodb.com/pictures2/portraits/10/050/10466.jpg', 'Agility, Longevity, Marksmanship, Natural Weapons, Stealth, Super Strength, Weapons Master', 5);
INSERT INTO hero(id, name, otherName, picture, powers, level)
VALUES (2, 'Angel Salvadore', 'Angel Salvadore Bohusk', 'https://www.superherodb.com/pictures2/portraits/10/050/1406.jpg', 'Animal Attributes, Animal Oriented Powers, Flight, Regeneration, Toxin and Disease Control', 4);
INSERT INTO hero(id, name, otherName, picture, powers, level)
VALUES (3, 'Bill Harken', '', 'https://www.superherodb.com/pictures2/portraits/10/050/1527.jpg', 'Super Speed, Super Strength, Toxin and Disease Resistance', 6);
-- end::adocSQL[]
INSERT INTO hero(id, name, otherName, picture, powers, level)
VALUES (4, 'Galadriel', '', 'https://www.superherodb.com/pictures2/portraits/11/050/11796.jpg', 'Danger Sense, Immortality, Intelligence, Invisibility, Magic, Precognition, Telekinesis, Telepathy', 17);
alter sequence hero_SEQ restart with 5;