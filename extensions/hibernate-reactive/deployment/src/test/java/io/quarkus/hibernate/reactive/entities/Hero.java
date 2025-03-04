package io.quarkus.hibernate.reactive.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity(name = "Hero")
@Table(name = "hero")
public class Hero {

    @jakarta.persistence.Id
    @jakarta.persistence.GeneratedValue
    public java.lang.Long id;

    @Column(unique = true)
    public String name;

    public String otherName;

    public int level;

    public String picture;

    @Column(columnDefinition = "TEXT")
    public String powers;

}