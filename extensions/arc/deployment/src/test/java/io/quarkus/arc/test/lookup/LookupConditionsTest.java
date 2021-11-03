package io.quarkus.arc.test.lookup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.lookup.LookupIfProperty;
import io.quarkus.arc.lookup.LookupUnlessProperty;
import io.quarkus.test.QuarkusUnitTest;

public class LookupConditionsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Foo.class, ServiceAlpha.class, ServiceBravo.class, ServiceCharlie.class))
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

    // -> suppressed because service.alpha.enabled=false
    @LookupUnlessProperty(name = "service.alpha.enabled", stringValue = "false")
    @Singleton
    static class ServiceAlpha implements Service {

        public String ping() {
            return "alpha";
        }
    }

    // -> NOT suppressed because service.bravo.enabled is not specified and lookupIfMissing=true
    @LookupIfProperty(name = "service.bravo.enabled", stringValue = "false", lookupIfMissing = true)
    @Singleton
    static class ServiceBravo implements Service {

        public String ping() {
            return "bravo";
        }
    }

    // -> suppressed because the property is not specified at all and lookupIfMissing=false
    @LookupIfProperty(name = "service.charlie.enabled", stringValue = "false")
    @Dependent
    static class ServiceCharlie implements Service {

        public String ping() {
            return "charlie";
        }
    }

}
