package io.quarkus.jberet.deployment.people;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PeopleDatabase {

    private final List<Person> peopleList = new CopyOnWriteArrayList<>();

    public void addPerson(Person person) {
        peopleList.add(person);
    }

    public List<Person> getPeopleList() {
        return peopleList;
    }

}
