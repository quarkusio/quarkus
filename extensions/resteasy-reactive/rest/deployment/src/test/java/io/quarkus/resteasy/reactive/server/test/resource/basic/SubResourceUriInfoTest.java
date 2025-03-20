package io.quarkus.resteasy.reactive.server.test.resource.basic;

import static org.hamcrest.Matchers.equalTo;

import java.util.function.Supplier;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.UriInfo;

import org.jboss.resteasy.reactive.RestPath;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.reactive.server.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class SubResourceUriInfoTest {
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);
                    war.addClasses(UsersResource.class);
                    war.addClasses(UserResource.class);
                    war.addClasses(ContactResource.class);
                    war.addClasses(ResponseHolder.class);
                    return war;
                }
            });

    @Test
    public void basicTest() {
        RestAssured.given()
                .get("/users/userId/contacts/contactId")
                .then()
                .statusCode(200)
                .body(equalTo("{id=[userId]}{id=[contactId, userId]}{id=[contactId, userId]}"));
    }

    @RequestScoped
    @Path("users")
    public static class UsersResource {

        @Inject
        ResponseHolder responseHolder;

        @Inject
        UriInfo uriInfo;

        @Inject
        ResourceContext resourceContext;

        @Path("{id}")
        public UserResource get(@RestPath String id) {
            responseHolder.setResponse(responseHolder.getResponse() + uriInfo.getPathParameters().toString());
            return resourceContext.getResource(UserResource.class);
        }
    }

    @RequestScoped
    public static class UserResource {

        @Inject
        ResponseHolder responseHolder;
        @Inject
        UriInfo uriInfo;

        @Inject
        ResourceContext resourceContext;

        @Path("contacts/{id}")
        public ContactResource get(@RestPath String id) {
            responseHolder.setResponse(responseHolder.getResponse() + uriInfo.getPathParameters().toString());
            return resourceContext.getResource(ContactResource.class);
        }
    }

    @RequestScoped
    public static class ContactResource {

        @Inject
        ResponseHolder responseHolder;

        @Inject
        UriInfo uriInfo;

        @GET
        public String getName(@RestPath String id) {
            responseHolder.setResponse(responseHolder.getResponse() + uriInfo.getPathParameters().toString());
            return responseHolder.getResponse();
        }
    }

    @RequestScoped
    public static class ResponseHolder {
        String response = "";

        public String getResponse() {
            return response;
        }

        public void setResponse(String response) {
            this.response = response;
        }
    }
}
