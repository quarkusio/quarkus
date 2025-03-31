package io.quarkus.resteasy.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.net.URL;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;

class TestHttpEndpointJaxRsApplicationTest {
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(UsersResource.class);
                    war.addClasses(UserApplication.class);
                    return war;
                }
            });

    @TestHTTPEndpoint(UsersResource.class)
    @TestHTTPResource
    URL url;

    @Test
    void basicTest() {
        assertThat(url.getPath(), is("/user-api/users"));
    }

    @ApplicationScoped
    @Path("/users")
    public static class UsersResource {
        @GET
        public String getUserName() {
            return "John Doe";
        }
    }

    @ApplicationPath("user-api")
    public static class UserApplication extends Application {

    }
}
