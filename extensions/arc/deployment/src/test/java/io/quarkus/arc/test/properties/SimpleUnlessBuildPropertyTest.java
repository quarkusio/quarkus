package io.quarkus.arc.test.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.properties.UnlessBuildProperty;
import io.quarkus.test.QuarkusUnitTest;

public class SimpleUnlessBuildPropertyTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(PingService.class, AlphaService.class, BravoService.class))
            .overrideConfigKey("foo.bar", "baz");

    @Inject
    PingService pingService;

    @Test
    public void testInjection() {
        // AlphaService is ignored
        assertEquals(20, pingService.ping());
    }

    interface PingService {

        int ping();

    }

    @UnlessBuildProperty(name = "foo.bar", stringValue = "baz")
    @ApplicationScoped
    static class AlphaService implements PingService {

        @Override
        public int ping() {
            return 10;
        }

    }

    @UnlessBuildProperty(name = "foo.bar", stringValue = "qux")
    @ApplicationScoped
    static class BravoService implements PingService {

        @Override
        public int ping() {
            return 20;
        }

    }

}
