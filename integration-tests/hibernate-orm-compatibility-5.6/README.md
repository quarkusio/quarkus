# Hibernate ORM 5.6 database compatibility tests

## What is this?

These module test that Quarkus can indeed work with a database created by Hibernate ORM 5.6
when the following property is set:

```properties
quarkus.hibernate-orm.database.orm-compatibility.version = 5.6
```

## How does it work?

The tests need to execute on a database whose schema and data
was initialized with Hibernate ORM 5.6.

Everything is already set up to restore a dump on startup.

## How to update the tests?

If you add new tests and those changes require new entity mappings and/or data,
make sure to update the project `database-generator` accordingly
(same entity mapping as in your tests, in particular).
This project depends on Quarkus 2 and is used to generate a database.

Then, to update the dump, run `./update-dump.sh` from each DB directory (`mariadb`, `postgresql`, ...).
This will start a container, generate the database, and update the dump in `src/test/resources`.

## Why is `database-generator` not part of the build?

Because:

1. It doesn't need to. This project is only meant to be used to update dumps.
2. It depends on Quarkus 2, so adding it to the build would pollute the local Maven repository unnecessarily.
