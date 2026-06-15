package io.quarkus.vertx.customizers;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.vertx.deployment.spi.VertxBootstrapConsumerBuildItem;
import io.vertx.core.internal.VertxBootstrap;
import io.vertx.mutiny.core.Vertx;

public class BuildTimeVertxBootstrapCustomizerTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap
                    .create(JavaArchive.class))
            .addBuildChainCustomizer(builder -> {
                builder.addBuildStep(context -> {
                    context.produce(new VertxBootstrapConsumerBuildItem(MyVertxBootstrapCustomizer.INSTANCE, 3000));
                }).produces(VertxBootstrapConsumerBuildItem.class).build();
            });

    @Inject
    Vertx vertx;

    @Test
    public void testThatTheCustomizerIsCalled() {
        Assertions.assertThat(CALLED).isTrue();
    }

    public static final AtomicBoolean CALLED = new AtomicBoolean();

    public static class MyVertxBootstrapCustomizer implements Consumer<VertxBootstrap> {

        public static final MyVertxBootstrapCustomizer INSTANCE = new MyVertxBootstrapCustomizer();

        @Override
        public void accept(VertxBootstrap bootstrap) {
            Assertions.assertThat(bootstrap).isNotNull();
            CALLED.set(true);
        }
    }
}
