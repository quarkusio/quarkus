package io.quarkus.jberet.deployment.people;

import java.util.UUID;

import javax.batch.api.chunk.ItemProcessor;
import javax.enterprise.context.Dependent;
import javax.inject.Named;

/**
 * Generates a random password for each person.
 */
@Named
@Dependent
public class PeopleProcessor implements ItemProcessor {

    @Override
    public Object processItem(Object item) {
        Person person = (Person) item;
        person.setPassword(UUID.randomUUID().toString());
        return person;
    }

}
