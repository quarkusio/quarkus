package io.quarkus.vertx.customizers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.VertxOptionsCustomizer;
import io.vertx.core.VertxOptions;
import io.vertx.core.file.FileSystemOptions;
import io.vertx.mutiny.core.Vertx;

public class VertxOptionsCustomizerTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap
                    .create(JavaArchive.class).addClasses(MyCustomizer.class));

    @Inject
    Vertx vertx;

    @Inject
    MyCustomizer customizer;

    @Test
    public void testCustomizer() {
        Assertions.assertThat(customizer.wasInvoked()).isTrue();
        String test = vertx.fileSystem().createTempDirectoryAndAwait("test");
        Assertions.assertThat(test).contains("target", "test");
    }

    @ApplicationScoped
    public static class MyCustomizer implements VertxOptionsCustomizer {

        volatile boolean invoked;

        @Override
        public void accept(VertxOptions options) {
            invoked = true;
            options.setFileSystemOptions(new FileSystemOptions().setFileCacheDir("target"));
        }

        public boolean wasInvoked() {
            return invoked;
        }
    }
}
