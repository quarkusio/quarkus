package io.quarkus.rest.client.reactive.jsonb.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;
import java.util.Objects;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class SimpleJsonbTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withEmptyApplication();

    @TestHTTPResource
    URI uri;

    @Test
    void shouldConsumeJsonEntity() {
        var resultList = RestClientBuilder.newBuilder().baseUri(uri).build(JsonClient.class)
                .listDtos();
        assertThat(resultList).containsExactly(new Dto("foo", "bar"), new Dto("chocolate", "bar"));
    }

    @Path("/json")
    public interface JsonClient {

        @GET
        @Path("/list-dtos")
        @Produces(MediaType.APPLICATION_JSON)
        List<Dto> listDtos();
    }

    @Path("/json")
    public static class JsonResource {

        @GET
        @Produces(MediaType.APPLICATION_JSON)
        @Path("/list-dtos")
        public List<Dto> listDtos() {
            return List.of(new Dto("foo", "bar"), new Dto("chocolate", "bar"));
        }
    }

    public static class Dto {
        public String name;
        public String value;

        public Dto(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public Dto() {
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Dto dto = (Dto) o;
            return Objects.equals(name, dto.name) && Objects.equals(value, dto.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, value);
        }
    }
}
