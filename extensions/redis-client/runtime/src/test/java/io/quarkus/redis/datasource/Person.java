package io.quarkus.redis.datasource;

import java.util.Objects;

public class Person {
    public static final Person person0 = new Person("obiwan", "kenobi");
    public static final Person person1 = new Person("luke", "skywalker");
    public static final Person person2 = new Person("anakin", "skywalker");
    public static final Person person3 = new Person("jar jar", "bins");
    public static final Person person4 = new Person("greedo", "");
    public static final Person person5 = new Person("jabba", "desilijic tiure");
    public static final Person person6 = new Person("wedge", "antilles");
    public static final Person person7 = new Person("quarsh", "panaka");

    public String firstname;
    public String lastname;

    public Person(String firstname, String lastname) {
        this.firstname = firstname;
        this.lastname = lastname;
    }

    public Person() {
        // Used by the mapper.
    }

    @Override
    public String toString() {
        return "Person{" + "firstname='" + firstname + '\'' + ", lastname='" + lastname + '\'' + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Person person = (Person) o;
        return Objects.equals(firstname, person.firstname) && Objects.equals(lastname, person.lastname);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstname, lastname);
    }
}
