package io.quarkus.resteasy.reactive.server.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;

public class StringTestHTTPResourceWithPathParamsTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest().withApplicationRoot((jar) -> jar.addClass(UserResource.class));

    @TestHTTPEndpoint(UserResource.class)
    @TestHTTPResource("{userId}/order/{orderId}")
    String getUserOrderUrl;

    @Test
    void testGettingUserOrder() {
        int userId = 123;
        int orderId = 456;
        given().when().get(getUserOrderUrl, userId, orderId)
                .then().statusCode(200).body(equalTo(String.format("Order (%d) of user (%d)", userId, orderId)));
    }

    @Path("/user")
    public static class UserResource {
        @Path("{userId}/order/{orderId}")
        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String getUserOrder(@PathParam("userId") int userId, @PathParam("orderId") int orderId) {
            return String.format("Order (%d) of user (%d)", userId, orderId);
        }
    }
}
