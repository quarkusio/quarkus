DELETE FROM Gift WHERE id=100000;
-- on multiple lines to properly test the multi-line SQL extractor
INSERT INTO Gift (id, name)
        VALUES (100000,'Teddy bear');
