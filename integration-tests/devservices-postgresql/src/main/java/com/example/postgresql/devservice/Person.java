package com.example.postgresql.devservice;

import javax.persistence.Entity;
import javax.persistence.Table;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Table
@Entity(name = "persons")
public class Person extends PanacheEntity {

    public String name;
    public Integer age;
}
