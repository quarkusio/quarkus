package io.quarkus.it.vertx;

import static io.quarkus.vertx.web.Route.HttpMethod.GET;
import static io.quarkus.vertx.web.Route.HttpMethod.POST;

import io.quarkus.vertx.web.Body;
import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RouteBase;
import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

@RouteBase(path = "/simple")
public class SimpleEndpoint {

    @Route(path = "person", methods = GET)
    public Person getPerson() {
        Person person = new Person();
        person.setName("Jan");
        return person;
    }

    @Route(path = "pet", methods = GET)
    public Uni<Pet> getPet() {
        Pet pet = new Pet();
        pet.setName("Jack");
        return Uni.createFrom().item(pet);
    }

    @Route(path = "pong", methods = GET)
    public JsonObject getPong() {
        return new JsonObject().put("name", "ping");
    }

    @Route(path = "data", methods = POST, produces = "application/json")
    public Buffer createData(@Body Data data) {
        data.setName(data.getName() + data.getName());
        return JsonObject.mapFrom(data).toBuffer();
    }

    public static class Person {

        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

    }

    public static class Pet {

        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

    }

    public static class Data {

        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

    }
}
