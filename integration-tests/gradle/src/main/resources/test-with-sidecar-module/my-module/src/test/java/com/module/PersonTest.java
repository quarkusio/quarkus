package com.module;

import org.junit.jupiter.api.Test;

import com.module.test.PersonFactory;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class PersonTest {

    @Test
    void test() {
        PersonFactory.newRandomPerson();
    }
}
