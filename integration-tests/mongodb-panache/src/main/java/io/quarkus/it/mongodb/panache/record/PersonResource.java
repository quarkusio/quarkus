package io.quarkus.it.mongodb.panache.record;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

@Path("/persons/record")
public class PersonResource {
    @Inject
    PersonRecordRepository personRecordRepository;

    @GET
    public List<PersonName> getPersons() {
        return personRecordRepository.findAll().project(PersonName.class).list();
    }

    @POST
    public void addPerson(PersonRecord person) {
        personRecordRepository.persist(person);
    }
}
