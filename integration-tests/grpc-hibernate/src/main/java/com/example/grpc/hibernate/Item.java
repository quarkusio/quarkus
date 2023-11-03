package com.example.grpc.hibernate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity(name = "Item")
public class Item {
    @Id
    @GeneratedValue
    public Long id;

    public String text;

    @Override
    public String toString() {
        return "Item{" +
                "id=" + id +
                ", text='" + text + '\'' +
                '}';
    }
}
