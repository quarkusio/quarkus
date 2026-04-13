package io.quarkus.vertx.customizers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.vertx.deployment.spi.VertxOptionsConsumerBuildItem;
import io.vertx.core.VertxOptions;

public class BuildTimeVertxOptionsCustomizerSamePriorityTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap
                    .create(JavaArchive.class))
            .addBuildChainCustomizer(builder -> {
                builder.addBuildStep(context -> {
                    context.produce(new VertxOptionsConsumerBuildItem(CustomizerWithPriority1000.INSTANCE, 1000));
                }).produces(VertxOptionsConsumerBuildItem.class).build();

                builder.addBuildStep(context -> {
                    context.produce(new VertxOptionsConsumerBuildItem(AnotherCustomizerWithPriority1000.INSTANCE, 1000));
                }).produces(VertxOptionsConsumerBuildItem.class).build();

            })
            .setLogRecordPredicate(lr -> lr.getLevel().intValue() >= Level.WARNING.intValue())
            .assertLogRecords(logRecords -> {
                assertThat(logRecords).anySatisfy(logRecord -> logRecord.getMessage().contains(
                        "Two VertxOptionsConsumerBuildItems have the same priority (1000)."));
            });

    @Test
    public void testThatTheCustomizersAreCalledInOrder() {
        assertThat(calls).containsExactly("1000", "1000");
    }

    public static final List<String> calls = new ArrayList<>();

    public static class CustomizerWithPriority1000 implements Consumer<VertxOptions> {

        public static final CustomizerWithPriority1000 INSTANCE = new CustomizerWithPriority1000();

        @Override
        public void accept(VertxOptions options) {
            assertThat(options).isNotNull();
            calls.add("1000");
        }
    }

    public static class AnotherCustomizerWithPriority1000 implements Consumer<VertxOptions> {

        public static final AnotherCustomizerWithPriority1000 INSTANCE = new AnotherCustomizerWithPriority1000();

        @Override
        public void accept(VertxOptions options) {
            assertThat(options).isNotNull();
            calls.add("1000");
        }
    }
}
