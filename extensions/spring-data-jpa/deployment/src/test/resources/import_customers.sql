INSERT INTO Customer$Country (id, name, isocode)
VALUES
    (1, 'United States', 'US'),
    (2, 'France', 'FR'),
    (3, 'Germany', 'DE'),
    (4, 'Spain', 'ES');

INSERT INTO Customer$Address (id, zipcode, country_id)
VALUES
    (1, '10001', 1),   -- US address
    (2, '75001', 2),   -- French address
    (3, '10115', 3),   -- German address
    (4, '28004', 4);   -- Spanish address

INSERT INTO Customer (id, name, age, birthdate, active, address_id)
VALUES
    (1,'Adam', 25, now(), TRUE,4),
    (2,'Adam', 20, now(), FALSE, 2),
    (3,'Eve', 20, now(), TRUE, 1),
    (4,null, 30, now(), FALSE,3),
    (5,'Tim', 38, now(), FALSE, 4),
    (6,'Timothée', 19, now(), FALSE, 2);