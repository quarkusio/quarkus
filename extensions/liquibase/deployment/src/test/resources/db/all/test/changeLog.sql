--liquibase formatted sql

--changeset dev:create-tables-1
create table LIQUIBASE (
    ID varchar(255) primary key,
    NAME varchar(255)
);

--changeset dev:test-1
create table TEST (
    ID varchar(255) primary key,
    NAME varchar(255)
);
