package io.quarkus.rest.client.reactive.jackson.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.UUID;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.annotation.JsonView;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.mutiny.Uni;

public class JsonViewClientTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().withEmptyApplication();

    @TestHTTPResource
    URI uri;

    @Test
    public void test() {
        UserClient client = QuarkusRestClientBuilder.newBuilder().baseUri(uri).build(UserClient.class);

        String id = UUID.randomUUID().toString();
        User u = client.get(id);
        assertThat(u.name).isEqualTo("bob");
        assertThat(u.id).isNull();

        u = client.getPrivate(id);
        assertThat(u.name).isEqualTo("bob");
        assertThat(u.id).isEqualTo(id);

        // Ensure JsonView also applies when resource is wrapped in Uni<>
        User u2 = client.getUni(id).await().indefinitely();
        assertThat(u2.name).isEqualTo("bob");
        assertThat(u2.id).isNull();

        User toCreate = new User();
        toCreate.id = "should-be-ignored";
        toCreate.name = "alice";
        Response resp = client.create(toCreate, false);
        assertThat(resp.getStatus()).isEqualTo(200);

        toCreate = new User();
        toCreate.id = "1";
        toCreate.name = "alice";
        resp = client.createPrivate(toCreate, true);
        assertThat(resp.getStatus()).isEqualTo(200);
    }

    public static class Views {
        public static class Public {
        }

        public static class Private extends Public {
        }
    }

    public static class User {
        @JsonView(Views.Private.class)
        public String id;

        @JsonView(Views.Public.class)
        public String name;
    }

    @Path("/users")
    public static class UserResource {
        @GET
        @Path("/{id}")
        @Produces(MediaType.APPLICATION_JSON)
        public User get(@RestPath String id) {
            User u = new User();
            u.id = id;
            u.name = "bob";
            return u;
        }

        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        public Response create(User user, @RestQuery @DefaultValue("false") boolean useId) {
            if ((user.id != null) && !useId) {
                return Response.status(Response.Status.BAD_REQUEST).entity("id must be null").build();
            } else if (user.id == null && useId) {
                return Response.status(Response.Status.BAD_REQUEST).entity("id must not be null").build();
            }
            if (user.name == null) {
                return Response.status(Response.Status.BAD_REQUEST).entity("name must be set").build();
            }
            return Response.ok().build();
        }
    }

    @Path("/users")
    @RegisterRestClient
    public interface UserClient {
        @GET
        @Path("/{id}")
        @Produces(MediaType.APPLICATION_JSON)
        @JsonView(Views.Public.class)
        User get(@RestPath String id);

        @GET
        @Path("/{id}")
        @Produces(MediaType.APPLICATION_JSON)
        @JsonView(Views.Private.class)
        User getPrivate(@RestPath String id);

        @GET
        @Path("/{id}")
        @Produces(MediaType.APPLICATION_JSON)
        @JsonView(Views.Public.class)
        Uni<User> getUni(@RestPath String id);

        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        Response create(@JsonView(Views.Public.class) User user, @RestQuery boolean useId);

        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        Response createPrivate(@JsonView(Views.Private.class) User user, @RestQuery boolean useId);
    }
}
