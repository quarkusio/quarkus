package io.quarkus.vertx.customizers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.vertx.VertxOptionsCustomizer;
import io.vertx.core.VertxOptions;

public class VertxOptionsCustomizerTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap
                    .create(JavaArchive.class).addClasses(MyCustomizer.class));

    @Inject
    MyCustomizer customizer;

    @Test
    public void testCustomizer() {
        Assertions.assertThat(customizer.wasInvoked()).isTrue();
    }

    @ApplicationScoped
    public static class MyCustomizer implements VertxOptionsCustomizer {

        volatile boolean invoked;

        @Override
        public void accept(VertxOptions options) {
            invoked = true;

        }

        public boolean wasInvoked() {
            return invoked;
        }
    }
}
