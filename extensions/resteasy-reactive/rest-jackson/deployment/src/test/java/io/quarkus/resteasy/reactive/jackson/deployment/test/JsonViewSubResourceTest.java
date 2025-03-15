package io.quarkus.resteasy.reactive.jackson.deployment.test;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.annotation.JsonView;

import io.quarkus.test.QuarkusUnitTest;

public class JsonViewSubResourceTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(User.class, Views.class, UsersResource.class, PublicUserResource.class,
                                    PrivateUserResource.class, MixedUserResource.class, SerializeDeserializeUserResource.class);
                }
            });

    @Test
    public void test() {
        given().accept("application/json").get("users/public")
                .then()
                .statusCode(200)
                .body(not(containsString("1")), containsString("test"));

        given().accept("application/json").get("users/private")
                .then()
                .statusCode(200)
                .body(containsString("1"), containsString("test"));

        given().accept("application/json").get("users/mixed")
                .then()
                .statusCode(200)
                .body(containsString("1"), containsString("test"));

        given().accept("application/json")
                .contentType("application/json")
                .body("""
                        {
                         "id": 1,
                         "name": "Foo"
                        }
                        """)
                .post("users/serialize-deserialize")
                .then()
                .statusCode(201)
                .body("id", equalTo(0))
                .body("name", equalTo("Foo"));
    }

    @Path("users")
    public static class UsersResource {

        @Path("public")
        public PublicUserResource getPublic() {
            return new PublicUserResource();
        }

        @Path("private")
        public PrivateUserResource getPrivate() {
            return new PrivateUserResource();
        }

        @Path("mixed")
        public MixedUserResource getMixed() {
            return new MixedUserResource();
        }

        @Path("serialize-deserialize")
        public SerializeDeserializeUserResource getSerializeDeserialize() {
            return new SerializeDeserializeUserResource();
        }
    }

    @JsonView(Views.Public.class)
    public static class PublicUserResource {
        @GET
        public User get() {
            return User.testUser();
        }
    }

    @JsonView(Views.Private.class)
    public static class PrivateUserResource {
        @GET
        public User get() {
            return User.testUser();
        }
    }

    @JsonView(Views.Public.class)
    public static class MixedUserResource {
        @GET
        @JsonView(Views.Private.class)
        public User get() {
            return User.testUser();
        }
    }

    public static class SerializeDeserializeUserResource {
        @POST
        @JsonView(Views.Private.class)
        public RestResponse<User> get(@JsonView(Views.Public.class) User user) {
            return RestResponse.status(CREATED, user);
        }
    }
}
