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
import io.quarkus.vertx.deployment.spi.VertxOptionsConsumerBuildItem;
import io.vertx.core.VertxOptions;
import io.vertx.mutiny.core.Vertx;

public class BuildTimeVertxOptionsCustomizerTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap
                    .create(JavaArchive.class))
            .addBuildChainCustomizer(builder -> {
                builder.addBuildStep(context -> {
                    context.produce(new VertxOptionsConsumerBuildItem(MyVertxOptionCustomizer.INSTANCE, 3000));
                }).produces(VertxOptionsConsumerBuildItem.class).build();
            });

    @Inject
    Vertx vertx;

    @Test
    public void testThatTheCustomizerIsCalled() {
        Assertions.assertThat(CALLED).isTrue();
    }

    public static final AtomicBoolean CALLED = new AtomicBoolean();

    public static class MyVertxOptionCustomizer implements Consumer<VertxOptions> {

        public static final MyVertxOptionCustomizer INSTANCE = new MyVertxOptionCustomizer();

        @Override
        public void accept(VertxOptions options) {
            Assertions.assertThat(options).isNotNull();
            CALLED.set(true);
        }
    }
}
