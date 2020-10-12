package io.quarkus.arc.test.contexts.stateless;

import io.quarkus.arc.Managed;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

@Singleton
public class PersonManager {

    private Person person;

    @Produces
    @Managed
    public Person getPerson() {
        return person;
    }

    public PersonManager setPerson(Person person) {
        this.person = person;
        return this;
    }
}
