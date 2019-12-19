package io.quarkus.jberet.deployment.people;

import java.io.Serializable;
import java.util.List;

import javax.batch.api.chunk.ItemWriter;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

@Dependent
@Named
public class PeopleWriter implements ItemWriter {

    @Inject
    PeopleDatabase peopleDatabase;

    @Override
    public void open(Serializable checkpoint) {
    }

    @Override
    public void close() {
    }

    @Override
    public void writeItems(List<Object> items) {
        for (Object person : items) {
            peopleDatabase.addPerson((Person) person);
        }
    }

    @Override
    public Serializable checkpointInfo() {
        return null;
    }

}
