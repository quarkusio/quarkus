package io.quarkus.it.panache.next;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PersonService {

    @Inject
    Person.Repository personRepository;

    public Person findById(Long id) {
        return personRepository.findById(id);
    }

    public long count() {
        return personRepository.count();
    }
}
