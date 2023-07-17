package com.example.reactive;

import jakarta.persistence.Entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;

@Entity
public class Item extends PanacheEntity {
    public String text;
}
