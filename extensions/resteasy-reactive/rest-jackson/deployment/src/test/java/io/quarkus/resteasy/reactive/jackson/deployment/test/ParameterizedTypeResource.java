package io.quarkus.resteasy.reactive.jackson.deployment.test;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/parameterized-type")
public class ParameterizedTypeResource {

    public enum Difficulty {
        EASY,
        HARD
    }

    public record ChildDto(String label) {
    }

    public static class Holder<T> {
        private T value;

        public T getValue() {
            return value;
        }

        public void setValue(T value) {
            this.value = value;
        }
    }

    public record ThingRequest(
            Holder<Difficulty> difficulty,
            Holder<ChildDto> child) {
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String create(ThingRequest request) {
        String diff = request.difficulty() != null && request.difficulty().getValue() != null
                ? request.difficulty().getValue().name()
                : "absent";
        String kid = request.child() != null && request.child().getValue() != null
                ? request.child().getValue().label()
                : "absent";
        return "{\"difficulty\":\"" + diff + "\",\"child\":\"" + kid + "\"}";
    }
}
