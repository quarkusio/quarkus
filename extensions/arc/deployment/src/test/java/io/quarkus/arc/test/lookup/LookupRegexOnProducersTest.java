package io.quarkus.arc.test.lookup;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.lookup.LookupIfProperty;
import io.quarkus.arc.lookup.LookupUnlessProperty;
import io.quarkus.arc.properties.StringValueMatch;
import io.quarkus.test.QuarkusExtensionTest;

public class LookupRegexOnProducersTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Client.class, Service.class, ServiceProducer.class))
            .overrideConfigKey("service.version", "1.5.3");

    @Inject
    Client client;

    @Test
    public void testProducerRegexMatch() {
        // producer "alpha" uses regex that matches "1.5.3" -> NOT suppressed
        assertThat(client.pingService()).isEqualTo("alpha");
    }

    @Singleton
    static class Client {

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

        // regex matches "1.5.3" -> NOT suppressed
        @LookupIfProperty(name = "service.version", stringValue = "^1\\.\\d+\\.\\d+$", match = StringValueMatch.REGEX)
        @Produces
        public Service alpha() {
            return () -> "alpha";
        }

        // regex matches "1.5.3" -> suppressed (unless = exclude on match)
        @LookupUnlessProperty(name = "service.version", stringValue = "^1\\..*", match = StringValueMatch.REGEX)
        @Produces
        public Service bravo() {
            return () -> "bravo";
        }
    }
}
