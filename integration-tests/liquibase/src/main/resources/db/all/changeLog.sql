--liquibase formatted sql

--changeset dev:all-sql-files-create-tables-1
create table ALL_SQL_FILES_LIQUIBASE (
    ID varchar(255) primary key,
    NAME varchar(255)
);
