--liquibase formatted sql

--changeset dev:sql-create-tables-1
create table SQL_LIQUIBASE (
    ID varchar(255) primary key,
    NAME varchar(255)
);

--changeset dev:sql-test-1
create table SQL_TEST (
    ID varchar(255) primary key,
    NAME varchar(255)
);
