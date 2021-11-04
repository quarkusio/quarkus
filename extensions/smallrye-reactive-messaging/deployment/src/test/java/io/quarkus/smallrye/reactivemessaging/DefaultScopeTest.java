package io.quarkus.smallrye.reactivemessaging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.Bean;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class DefaultScopeTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(NoScope.class, NoScopeButResource.class));

    @Inject
    NoScope noScope;

    @Inject
    NoScopeButResource resource;

    @Test
    public void testDefaultScope() {
        assertEquals(Dependent.class, noScope.getBean().getScope());
        assertEquals(Singleton.class, resource.getBean().getScope());
    }

    public static class NoScope {

        @Inject
        Bean<NoScope> bean;

        public Bean<NoScope> getBean() {
            return bean;
        }

        @Incoming("source")
        public String toUpperCase(String payload) {
            return payload.toUpperCase();
        }
    }

    @Path("foo")
    public static class NoScopeButResource {

        @Inject
        Bean<NoScope> bean;

        public Bean<NoScope> getBean() {
            return bean;
        }

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String hello() {
            return "hello";
        }

        @Incoming("source")
        public String toUpperCase(String payload) {
            return payload.toUpperCase();
        }
    }

}
