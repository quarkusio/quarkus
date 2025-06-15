package io.quarkus.arc.test.observer.injectionpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.enterprise.event.Observes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

public class ObserverInjectingDependentSyntheticBeanWithMetadataTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addClasses(MyConfig.class)).overrideConfigKey("my.config.value1", "42");

    @Test
    public void testInjection() {

    }

    @ConfigMapping(prefix = "my.config")
    public interface MyConfig {

        int value1();

        @WithDefault("baz")
        String value2();

    }

    public static class MyBean {

        void onStart(@Observes StartupEvent event, MyConfig config) {
            // A @Dependent synthetic bean is registered for MyConfig, and it attempts to obtain InjectionPoint in its
            // create() method
            assertEquals(42, config.value1());
            assertEquals("baz", config.value2());
        }

    }

}
