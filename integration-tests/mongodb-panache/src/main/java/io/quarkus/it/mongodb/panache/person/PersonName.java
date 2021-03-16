package io.quarkus.it.mongodb.panache.person;

import java.util.Objects;

public class PersonName {
    public String lastname;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PersonName that = (PersonName) o;
        return Objects.equals(lastname, that.lastname);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lastname);
    }
}
