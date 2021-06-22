package com.example.grpc.hibernate;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity(name = "Item")
public class Item {
    @Id
    @GeneratedValue
    public Long id;

    public String text;
}