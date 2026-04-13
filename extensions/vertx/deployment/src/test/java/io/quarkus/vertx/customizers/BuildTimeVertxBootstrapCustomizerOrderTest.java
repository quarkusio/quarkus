package io.quarkus.vertx.customizers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.vertx.deployment.spi.VertxBootstrapConsumerBuildItem;
import io.vertx.core.internal.VertxBootstrap;

public class BuildTimeVertxBootstrapCustomizerOrderTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap
                    .create(JavaArchive.class))
            .addBuildChainCustomizer(builder -> {
                builder.addBuildStep(context -> {
                    context.produce(new VertxBootstrapConsumerBuildItem(CustomizerWithPriority1000.INSTANCE, 1000));
                }).produces(VertxBootstrapConsumerBuildItem.class).build();

                builder.addBuildStep(context -> {
                    context.produce(new VertxBootstrapConsumerBuildItem(CustomizerWithPriority2000.INSTANCE, 2000));
                }).produces(VertxBootstrapConsumerBuildItem.class).build();

            });

    @Test
    public void testThatTheCustomizersAreCalledInOrder() {
        assertThat(calls).containsExactly("1000", "2000");
    }

    public static final List<String> calls = new ArrayList<>();

    public static class CustomizerWithPriority1000 implements Consumer<VertxBootstrap> {

        public static final CustomizerWithPriority1000 INSTANCE = new CustomizerWithPriority1000();

        @Override
        public void accept(VertxBootstrap bootstrap) {
            assertThat(bootstrap).isNotNull();
            calls.add("1000");
        }
    }

    public static class CustomizerWithPriority2000 implements Consumer<VertxBootstrap> {

        public static final CustomizerWithPriority2000 INSTANCE = new CustomizerWithPriority2000();

        @Override
        public void accept(VertxBootstrap bootstrap) {
            assertThat(bootstrap).isNotNull();
            calls.add("2000");
        }
    }
}
