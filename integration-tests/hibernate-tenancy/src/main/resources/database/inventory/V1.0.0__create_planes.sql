CREATE TABLE plane
(
  id   INT,
  name VARCHAR(255)
);
CREATE SEQUENCE plane_id_seq START WITH 3;
INSERT INTO plane(id, name) VALUES (1, 'Airbus A320');
INSERT INTO plane(id, name) VALUES (2, 'Airbus A350');
