-- MariaDB dumps lead to weird values in sequences because of caching mechanisms.
-- => Reset sequences to the value they would have had, had we not dumped and restored the database.

ALTER SEQUENCE `hibernate_sequence` restart;
-- We created exactly two entities
SELECT nextval(`hibernate_sequence`);
SELECT nextval(`hibernate_sequence`);

ALTER SEQUENCE `gengendefallocsize` restart;
-- We created exactly two entities
SELECT nextval(`gengendefallocsize`);
SELECT nextval(`gengendefallocsize`);

ALTER SEQUENCE `seqgendefallocsize` restart;
-- We created exactly two entities
-- This sequence uses a pooled optimizer, but the first two entities still require two calls to nextval()
SELECT nextval(`seqgendefallocsize`);
SELECT nextval(`seqgendefallocsize`);
