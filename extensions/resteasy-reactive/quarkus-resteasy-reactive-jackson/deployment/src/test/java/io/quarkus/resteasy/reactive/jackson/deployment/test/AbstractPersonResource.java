package io.quarkus.resteasy.reactive.jackson.deployment.test;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

abstract class AbstractPersonResource {

    @Path("abstract-with-security")
    @GET
    public Person abstractPerson() {
        Person person = new Person();
        person.setFirst("Bob");
        person.setLast("Builder");
        return person;
    }
}
