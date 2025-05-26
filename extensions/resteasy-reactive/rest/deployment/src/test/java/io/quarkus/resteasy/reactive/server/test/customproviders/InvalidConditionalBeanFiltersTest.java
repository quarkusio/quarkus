package io.quarkus.resteasy.reactive.server.test.customproviders;

import static org.junit.jupiter.api.Assertions.*;

import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.UriInfo;

import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.test.QuarkusUnitTest;

public class InvalidConditionalBeanFiltersTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(TestResource.class, Filters.class);
                }
            }).assertException(t -> {
                String message = t.getMessage();
                assertTrue(message.contains("@IfBuildProfile"));
                assertTrue(message.contains("request"));
                assertTrue(message.contains(InvalidConditionalBeanFiltersTest.Filters.class.getName()));
            });

    @Test
    public void test() {
        fail("Should never have been called");
    }

    @Path("test")
    public static class TestResource {

        @GET
        public String hello() {
            return "hello";
        }

    }

    public static class Filters {

        @IfBuildProfile("test")
        @ServerRequestFilter
        public void request(UriInfo info) {

        }

    }
}
