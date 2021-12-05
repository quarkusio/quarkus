package io.quarkus.arc.test.lookup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.lookup.LookupIfProperty;
import io.quarkus.arc.lookup.LookupUnlessProperty;
import io.quarkus.test.QuarkusUnitTest;

public class LookupConditionOnProducersTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Foo.class, Service.class, ServiceProducer.class))
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

    @Singleton
    static class ServiceProducer {

        // -> suppressed because service.alpha.enabled=false
        @LookupIfProperty(name = "service.alpha.enabled", stringValue = "true")
        @Produces
        public Service alpha() {
            return new Service() {

                @Override
                public String ping() {
                    return "alpha";
                }
            };
        }

        // -> NOT suppressed because the property is not specified and lookUpIfMissing=true
        @LookupUnlessProperty(name = "service.bravo.enabled", stringValue = "false", lookupIfMissing = true)
        @Produces
        public Service bravo() {
            return new Service() {

                @Override
                public String ping() {
                    return "bravo";
                }
            };
        }

    }

}
