package io.quarkus.arc.test.lookup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.lookup.LookupIfProperty;
import io.quarkus.arc.lookup.LookupUnlessProperty;
import io.quarkus.test.QuarkusUnitTest;

public class LookupConditionsCombinedTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Foo.class, ServiceAlpha.class, ServiceBravo.class))
            .overrideConfigKey("service.alpha.enabled", "false");

    @Inject
    Foo foo;

    @Test
    public void testConditions() {
        assertEquals("bravo", foo.pingService());
    }

    @Singleton
    static class Foo {

        @Inject
        Instance<Service> service;

        String pingService() {
            return service.get().ping();
        }
    }

    interface Service {

        String ping();

    }

    @LookupIfProperty(name = "service.alpha.enabled", stringValue = "false")
    @LookupUnlessProperty(name = "service.alpha.enabled", stringValue = "false")
    @Singleton
    static class ServiceAlpha implements Service {

        public String ping() {
            return "alpha";
        }
    }

    @Singleton
    static class ServiceBravo implements Service {

        public String ping() {
            return "bravo";
        }
    }
}
