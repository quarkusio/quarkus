package io.quarkus.redis.it;

public class Person {

    public final String firstName;
    public final String lastName;

    public Person(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }
}
