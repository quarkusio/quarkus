CREATE TABLE plane
(
  id   INT,
  name VARCHAR(40)
);
CREATE SEQUENCE plane_iq_seq START WITH 3;
INSERT INTO plane(id, name) VALUES (1, 'Boeing 737');
INSERT INTO plane(id, name) VALUES (2, 'Boeing 747');
