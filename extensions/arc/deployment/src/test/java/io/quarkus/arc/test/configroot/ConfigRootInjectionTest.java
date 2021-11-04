package io.quarkus.arc.test.configroot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.ApplicationConfig;
import io.quarkus.runtime.ThreadPoolConfig;
import io.quarkus.test.QuarkusUnitTest;

public class ConfigRootInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ConfigRootInjectionTest.class, Client.class));

    @Inject
    Client client;

    @Test
    public void testInjectionWorks() {
        assertNotNull(client.applicationConfig);
        assertNotNull(client.applicationConfig.name);
        assertEquals(1, client.threadPoolConfig.coreThreads);
    }

    @Singleton
    static class Client {

        @Inject
        ApplicationConfig applicationConfig;

        @Inject
        ThreadPoolConfig threadPoolConfig;

    }

}
