package io.quarkus.resteasy.reactive.jackson.deployment.test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

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
