package com.module.test;

import java.util.UUID;

import com.module.Person;

public class PersonFactory {

    public static Person newRandomPerson() {
        return new Person(UUID.randomUUID().toString());
    }
}
