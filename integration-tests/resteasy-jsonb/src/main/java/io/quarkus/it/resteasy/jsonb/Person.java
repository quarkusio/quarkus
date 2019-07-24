package io.quarkus.it.resteasy.jsonb;

// used to show that when there is a single implementation of an interface, a serializer is generated
public class Person implements HasName {

    private final String name;
    private final int age;

    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }

    @Override
    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }
}
