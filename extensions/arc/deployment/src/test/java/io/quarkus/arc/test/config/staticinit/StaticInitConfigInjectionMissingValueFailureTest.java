package io.quarkus.arc.test.config.staticinit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class StaticInitConfigInjectionMissingValueFailureTest {

    static final String PROPERTY_NAME = "static.init.missing.apfelstrudel";

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(StaticInitBean.class))
            .assertException(t -> {
                assertThat(t).isInstanceOf(IllegalStateException.class)
                        .hasMessageContainingAll(
                                "A runtime config property value differs from the value that was injected during the static intialization phase",
                                "the runtime value of '" + PROPERTY_NAME
                                        + "' is [gizmo] but the value [null] was injected into io.quarkus.arc.test.config.staticinit.StaticInitConfigInjectionMissingValueFailureTest$StaticInitBean#value");
            });

    @Test
    public void test() {
        fail();
    }

    @Singleton
    public static class StaticInitBean {

        @ConfigProperty(name = PROPERTY_NAME)
        Optional<String> value;

        // bean is instantiated during STATIC_INIT
        void onInit(@Observes @Initialized(ApplicationScoped.class) Object event) {
            System.setProperty(PROPERTY_NAME, "gizmo");
        }

    }

    @AfterAll
    static void afterAll() {
        System.clearProperty(PROPERTY_NAME);
    }
}
