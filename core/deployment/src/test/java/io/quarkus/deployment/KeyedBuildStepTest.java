package io.quarkus.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.builder.BuildChain;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildException;
import io.quarkus.builder.BuildResult;
import io.quarkus.builder.BuildStep;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.Key;
import io.quarkus.deployment.key.KeyUtils;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.QuarkusConfigFactory;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

public class KeyedBuildStepTest {

    @BeforeAll
    static void setupConfig() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withMapping(ExtensionLoaderConfig.class)
                .build();
        QuarkusConfigFactory.setConfig(config);
    }

    @AfterAll
    static void tearDownConfig() {
        QuarkusConfigFactory.setConfig(null);
    }

    // -- Test key types --
    public static class TestDS {
    }

    public static class OtherQualifier {
    }

    // -- Test build items --

    public static final class KeyedPropertyItem extends MultiBuildItem {
        @Key(TestDS.class)
        private final String dsName;
        private final String property;

        public KeyedPropertyItem(String dsName, String property) {
            this.dsName = dsName;
            this.property = property;
        }

        public String getDsName() {
            return dsName;
        }

        public String getProperty() {
            return property;
        }
    }

    public static final class OutputItem extends MultiBuildItem implements Comparable<OutputItem> {
        private final String value;

        public OutputItem(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public int compareTo(OutputItem o) {
            return value.compareTo(o.value);
        }
    }

    public static final class UnkeyedItem extends MultiBuildItem {
        private final String data;

        public UnkeyedItem(String data) {
            this.data = data;
        }

        public String getData() {
            return data;
        }
    }

    public static final class SharedConfigItem extends SimpleBuildItem {
        private final String config;

        public SharedConfigItem(String config) {
            this.config = config;
        }

        public String getConfig() {
            return config;
        }
    }

    // -- Test build step classes with @Key --

    public static class SimpleKeyedProcessor {
        @io.quarkus.deployment.annotations.BuildStep
        public List<OutputItem> process(@Key(TestDS.class) String name, List<KeyedPropertyItem> props) {
            List<OutputItem> result = new ArrayList<>();
            result.add(new OutputItem(name + ":" + props.size()));
            return result;
        }
    }

    public static class KeyedProcessorWithProducer {
        @io.quarkus.deployment.annotations.BuildStep
        public void produce(@Key(TestDS.class) String name, List<KeyedPropertyItem> props,
                BuildProducer<OutputItem> producer) {
            for (KeyedPropertyItem prop : props) {
                producer.produce(new OutputItem(name + "=" + prop.getProperty()));
            }
        }
    }

    public static class KeyedProcessorWithNonKeyed {
        @io.quarkus.deployment.annotations.BuildStep
        public List<OutputItem> process(@Key(TestDS.class) String name, List<KeyedPropertyItem> props,
                SharedConfigItem config) {
            List<OutputItem> result = new ArrayList<>();
            result.add(new OutputItem(name + ":" + props.size() + ":" + config.getConfig()));
            return result;
        }
    }

    public static class KeyedProcessorWithUnkeyedMulti {
        @io.quarkus.deployment.annotations.BuildStep
        public List<OutputItem> process(@Key(TestDS.class) String name, List<KeyedPropertyItem> props,
                List<UnkeyedItem> unkeyed) {
            List<OutputItem> result = new ArrayList<>();
            result.add(new OutputItem(name + ":props=" + props.size() + ":unkeyed=" + unkeyed.size()));
            return result;
        }
    }

    public static class SingularKeyedProcessor {
        @io.quarkus.deployment.annotations.BuildStep
        public List<OutputItem> process(@Key(TestDS.class) String name, KeyedPropertyItem prop) {
            return List.of(new OutputItem(name + "=" + prop.getProperty()));
        }
    }

    public static class MixedSingularAndListProcessor {
        @io.quarkus.deployment.annotations.BuildStep
        public List<OutputItem> process(@Key(TestDS.class) String name, KeyedPropertyItem prop,
                List<KeyedPropertyItem> allProps) {
            return List.of(new OutputItem(name + ":singular=" + prop.getProperty() + ":list=" + allProps.size()));
        }
    }

    public static class MultiKeyProcessor {
        @io.quarkus.deployment.annotations.BuildStep
        public List<OutputItem> process(@Key(TestDS.class) String name,
                @Key(OtherQualifier.class) String other, List<KeyedPropertyItem> props) {
            return List.of(new OutputItem(name));
        }
    }

    // -- BooleanSupplier implementations for condition tests --

    public static class AlwaysTrue implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            return true;
        }
    }

    public static class AlwaysFalse implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            return false;
        }
    }

    // -- Test build step classes with onlyIf / onlyIfNot --

    @BuildSteps(onlyIf = AlwaysTrue.class)
    public static class ClassOnlyIfTrueProcessor {
        @io.quarkus.deployment.annotations.BuildStep
        public List<OutputItem> process(@Key(TestDS.class) String name, List<KeyedPropertyItem> props) {
            return List.of(new OutputItem(name + ":" + props.size()));
        }
    }

    @BuildSteps(onlyIf = AlwaysFalse.class)
    public static class ClassOnlyIfFalseProcessor {
        @io.quarkus.deployment.annotations.BuildStep
        public List<OutputItem> process(@Key(TestDS.class) String name, List<KeyedPropertyItem> props) {
            return List.of(new OutputItem(name + ":" + props.size()));
        }
    }

    @BuildSteps(onlyIfNot = AlwaysTrue.class)
    public static class ClassOnlyIfNotTrueProcessor {
        @io.quarkus.deployment.annotations.BuildStep
        public List<OutputItem> process(@Key(TestDS.class) String name, List<KeyedPropertyItem> props) {
            return List.of(new OutputItem(name + ":" + props.size()));
        }
    }

    @BuildSteps(onlyIfNot = AlwaysFalse.class)
    public static class ClassOnlyIfNotFalseProcessor {
        @io.quarkus.deployment.annotations.BuildStep
        public List<OutputItem> process(@Key(TestDS.class) String name, List<KeyedPropertyItem> props) {
            return List.of(new OutputItem(name + ":" + props.size()));
        }
    }

    public static class MethodOnlyIfFalseProcessor {
        @io.quarkus.deployment.annotations.BuildStep
        public List<OutputItem> alwaysRuns(@Key(TestDS.class) String name, List<KeyedPropertyItem> props) {
            return List.of(new OutputItem("always:" + name));
        }

        @io.quarkus.deployment.annotations.BuildStep(onlyIf = AlwaysFalse.class)
        public List<OutputItem> neverRuns(@Key(TestDS.class) String name, List<KeyedPropertyItem> props) {
            return List.of(new OutputItem("never:" + name));
        }
    }

    public static class MethodOnlyIfNotTrueProcessor {
        @io.quarkus.deployment.annotations.BuildStep
        public List<OutputItem> alwaysRuns(@Key(TestDS.class) String name, List<KeyedPropertyItem> props) {
            return List.of(new OutputItem("always:" + name));
        }

        @io.quarkus.deployment.annotations.BuildStep(onlyIfNot = AlwaysTrue.class)
        public List<OutputItem> neverRuns(@Key(TestDS.class) String name, List<KeyedPropertyItem> props) {
            return List.of(new OutputItem("never:" + name));
        }
    }

    public static class FailingKeyedProcessor {
        @io.quarkus.deployment.annotations.BuildStep
        public List<OutputItem> process(@Key(TestDS.class) String name, List<KeyedPropertyItem> props) {
            throw new IllegalStateException("Failure for key: " + name);
        }
    }

    // -- Helper --

    private Consumer<BuildChainBuilder> loadSteps(Class<?> clazz) {
        return ExtensionLoader.loadStepsFromClass(clazz, new HashMap<>(), null);
    }

    private Consumer<BuildChainBuilder> loadStepsWithConditions(Class<?> clazz) {
        return ExtensionLoader.loadStepsFromClass(clazz, new HashMap<>(), createSupplierFactory());
    }

    private static BooleanSupplierFactoryBuildItem createSupplierFactory() {
        try {
            Constructor<BooleanSupplierFactoryBuildItem> ctor = BooleanSupplierFactoryBuildItem.class
                    .getDeclaredConstructor(LaunchMode.class, io.quarkus.dev.spi.DevModeType.class);
            ctor.setAccessible(true);
            return ctor.newInstance(LaunchMode.NORMAL, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // -- Tests --

    @Test
    void testSingleKeyFiltering() throws Exception {
        BuildChainBuilder builder = BuildChain.builder();

        // producer step: emit keyed items
        builder.addBuildStep(new BuildStep() {
            @Override
            public void execute(BuildContext bc) {
                bc.produce(new KeyedPropertyItem("a", "p1"));
                bc.produce(new KeyedPropertyItem("a", "p2"));
                bc.produce(new KeyedPropertyItem("b", "p3"));
            }
        }).produces(KeyedPropertyItem.class).build();

        // load the per-key build steps
        loadSteps(SimpleKeyedProcessor.class).accept(builder);

        builder.addFinal(OutputItem.class);
        BuildChain chain = builder.build();
        BuildResult result = chain.createExecutionBuilder("test").execute();

        List<OutputItem> outputs = result.consumeMulti(OutputItem.class);
        assertThat(outputs).extracting(OutputItem::getValue)
                .containsExactlyInAnyOrder("a:2", "b:1");
    }

    @Test
    void testBuildProducerInKeyedClass() throws Exception {
        BuildChainBuilder builder = BuildChain.builder();

        builder.addBuildStep(new BuildStep() {
            @Override
            public void execute(BuildContext bc) {
                bc.produce(new KeyedPropertyItem("x", "val1"));
                bc.produce(new KeyedPropertyItem("x", "val2"));
                bc.produce(new KeyedPropertyItem("y", "val3"));
            }
        }).produces(KeyedPropertyItem.class).build();

        loadSteps(KeyedProcessorWithProducer.class).accept(builder);

        builder.addFinal(OutputItem.class);
        BuildChain chain = builder.build();
        BuildResult result = chain.createExecutionBuilder("test").execute();

        List<OutputItem> outputs = result.consumeMulti(OutputItem.class);
        assertThat(outputs).extracting(OutputItem::getValue)
                .containsExactlyInAnyOrder("x=val1", "x=val2", "y=val3");
    }

    @Test
    void testNonKeyedItemsPassThrough() throws Exception {
        BuildChainBuilder builder = BuildChain.builder();

        // produce keyed items and a shared config
        builder.addBuildStep(new BuildStep() {
            @Override
            public void execute(BuildContext bc) {
                bc.produce(new KeyedPropertyItem("a", "p1"));
                bc.produce(new KeyedPropertyItem("b", "p2"));
                bc.produce(new SharedConfigItem("global"));
            }
        }).produces(KeyedPropertyItem.class).produces(SharedConfigItem.class).build();

        loadSteps(KeyedProcessorWithNonKeyed.class).accept(builder);

        builder.addFinal(OutputItem.class);
        BuildChain chain = builder.build();
        BuildResult result = chain.createExecutionBuilder("test").execute();

        List<OutputItem> outputs = result.consumeMulti(OutputItem.class);
        assertThat(outputs).extracting(OutputItem::getValue)
                .containsExactlyInAnyOrder("a:1:global", "b:1:global");
    }

    @Test
    void testNoKeyedItemsProducesNothing() throws Exception {
        BuildChainBuilder builder = BuildChain.builder();

        // produce nothing for the keyed type — but we need the chain to work,
        // so add a dummy step that produces the final item directly
        builder.addBuildStep(new BuildStep() {
            @Override
            public void execute(BuildContext bc) {
                bc.produce(new OutputItem("fallback"));
            }
        }).produces(OutputItem.class).build();

        loadSteps(SimpleKeyedProcessor.class).accept(builder);

        builder.addFinal(OutputItem.class);
        BuildChain chain = builder.build();
        BuildResult result = chain.createExecutionBuilder("test").execute();

        List<OutputItem> outputs = result.consumeMulti(OutputItem.class);
        // only the fallback item, keyed class produced nothing
        assertThat(outputs).extracting(OutputItem::getValue)
                .containsExactly("fallback");
    }

    @Test
    void testNonKeyedMultiPassesThrough() throws Exception {
        BuildChainBuilder builder = BuildChain.builder();

        builder.addBuildStep(new BuildStep() {
            @Override
            public void execute(BuildContext bc) {
                bc.produce(new KeyedPropertyItem("a", "p1"));
                bc.produce(new KeyedPropertyItem("b", "p2"));
                bc.produce(new UnkeyedItem("u1"));
                bc.produce(new UnkeyedItem("u2"));
                bc.produce(new UnkeyedItem("u3"));
            }
        }).produces(KeyedPropertyItem.class).produces(UnkeyedItem.class).build();

        loadSteps(KeyedProcessorWithUnkeyedMulti.class).accept(builder);

        builder.addFinal(OutputItem.class);
        BuildChain chain = builder.build();
        BuildResult result = chain.createExecutionBuilder("test").execute();

        List<OutputItem> outputs = result.consumeMulti(OutputItem.class);
        // each key gets all 3 unkeyed items
        assertThat(outputs).extracting(OutputItem::getValue)
                .containsExactlyInAnyOrder("a:props=1:unkeyed=3", "b:props=1:unkeyed=3");
    }

    @Test
    void testSingularKeyedParameter() throws Exception {
        BuildChainBuilder builder = BuildChain.builder();

        builder.addBuildStep(new BuildStep() {
            @Override
            public void execute(BuildContext bc) {
                bc.produce(new KeyedPropertyItem("a", "p1"));
                bc.produce(new KeyedPropertyItem("b", "p2"));
            }
        }).produces(KeyedPropertyItem.class).build();

        loadSteps(SingularKeyedProcessor.class).accept(builder);

        builder.addFinal(OutputItem.class);
        BuildChain chain = builder.build();
        BuildResult result = chain.createExecutionBuilder("test").execute();

        List<OutputItem> outputs = result.consumeMulti(OutputItem.class);
        assertThat(outputs).extracting(OutputItem::getValue)
                .containsExactlyInAnyOrder("a=p1", "b=p2");
    }

    @Test
    void testMixedSingularAndListParameter() throws Exception {
        BuildChainBuilder builder = BuildChain.builder();

        builder.addBuildStep(new BuildStep() {
            @Override
            public void execute(BuildContext bc) {
                bc.produce(new KeyedPropertyItem("a", "p1"));
                bc.produce(new KeyedPropertyItem("a", "p2"));
                bc.produce(new KeyedPropertyItem("b", "p3"));
            }
        }).produces(KeyedPropertyItem.class).build();

        loadSteps(MixedSingularAndListProcessor.class).accept(builder);

        builder.addFinal(OutputItem.class);
        BuildChain chain = builder.build();
        BuildResult result = chain.createExecutionBuilder("test").execute();

        List<OutputItem> outputs = result.consumeMulti(OutputItem.class);
        // singular gets the first matching item, list gets all matching items
        assertThat(outputs).extracting(OutputItem::getValue)
                .containsExactlyInAnyOrder("a:singular=p1:list=2", "b:singular=p3:list=1");
    }

    @Test
    void testMultipleKeyParametersOnMethodFails() {
        assertThatThrownBy(() -> loadSteps(MultiKeyProcessor.class))
                .hasMessageContaining("Multiple @Key parameters");
    }

    @Test
    void testMultipleKeyFieldsOnBuildItemFails() {
        // create an item class with two @Key fields inline
        assertThatThrownBy(() -> KeyUtils.findKeyField(DualKeyItem.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Multiple @Key fields");
    }

    // -- onlyIf / onlyIfNot tests --

    @Test
    void testBuildStepsOnlyIfTrue() throws Exception {
        BuildChainBuilder builder = BuildChain.builder();

        builder.addBuildStep(new BuildStep() {
            @Override
            public void execute(BuildContext bc) {
                bc.produce(new KeyedPropertyItem("a", "p1"));
                bc.produce(new KeyedPropertyItem("b", "p2"));
            }
        }).produces(KeyedPropertyItem.class).build();

        loadStepsWithConditions(ClassOnlyIfTrueProcessor.class).accept(builder);

        builder.addFinal(OutputItem.class);
        BuildResult result = builder.build().createExecutionBuilder("test").execute();

        List<OutputItem> outputs = result.consumeMulti(OutputItem.class);
        assertThat(outputs).extracting(OutputItem::getValue)
                .containsExactlyInAnyOrder("a:1", "b:1");
    }

    @Test
    void testBuildStepsOnlyIfFalse() throws Exception {
        BuildChainBuilder builder = BuildChain.builder();

        builder.addBuildStep(new BuildStep() {
            @Override
            public void execute(BuildContext bc) {
                bc.produce(new KeyedPropertyItem("a", "p1"));
                bc.produce(new OutputItem("fallback"));
            }
        }).produces(KeyedPropertyItem.class).produces(OutputItem.class).build();

        loadStepsWithConditions(ClassOnlyIfFalseProcessor.class).accept(builder);

        builder.addFinal(OutputItem.class);
        BuildResult result = builder.build().createExecutionBuilder("test").execute();

        List<OutputItem> outputs = result.consumeMulti(OutputItem.class);
        assertThat(outputs).extracting(OutputItem::getValue)
                .containsExactly("fallback");
    }

    @Test
    void testBuildStepsOnlyIfNotTrue() throws Exception {
        BuildChainBuilder builder = BuildChain.builder();

        builder.addBuildStep(new BuildStep() {
            @Override
            public void execute(BuildContext bc) {
                bc.produce(new KeyedPropertyItem("a", "p1"));
                bc.produce(new OutputItem("fallback"));
            }
        }).produces(KeyedPropertyItem.class).produces(OutputItem.class).build();

        loadStepsWithConditions(ClassOnlyIfNotTrueProcessor.class).accept(builder);

        builder.addFinal(OutputItem.class);
        BuildResult result = builder.build().createExecutionBuilder("test").execute();

        List<OutputItem> outputs = result.consumeMulti(OutputItem.class);
        assertThat(outputs).extracting(OutputItem::getValue)
                .containsExactly("fallback");
    }

    @Test
    void testBuildStepsOnlyIfNotFalse() throws Exception {
        BuildChainBuilder builder = BuildChain.builder();

        builder.addBuildStep(new BuildStep() {
            @Override
            public void execute(BuildContext bc) {
                bc.produce(new KeyedPropertyItem("a", "p1"));
                bc.produce(new KeyedPropertyItem("b", "p2"));
            }
        }).produces(KeyedPropertyItem.class).build();

        loadStepsWithConditions(ClassOnlyIfNotFalseProcessor.class).accept(builder);

        builder.addFinal(OutputItem.class);
        BuildResult result = builder.build().createExecutionBuilder("test").execute();

        List<OutputItem> outputs = result.consumeMulti(OutputItem.class);
        assertThat(outputs).extracting(OutputItem::getValue)
                .containsExactlyInAnyOrder("a:1", "b:1");
    }

    @Test
    void testBuildStepMethodOnlyIfFalse() throws Exception {
        BuildChainBuilder builder = BuildChain.builder();

        builder.addBuildStep(new BuildStep() {
            @Override
            public void execute(BuildContext bc) {
                bc.produce(new KeyedPropertyItem("a", "p1"));
                bc.produce(new KeyedPropertyItem("b", "p2"));
            }
        }).produces(KeyedPropertyItem.class).build();

        loadStepsWithConditions(MethodOnlyIfFalseProcessor.class).accept(builder);

        builder.addFinal(OutputItem.class);
        BuildResult result = builder.build().createExecutionBuilder("test").execute();

        List<OutputItem> outputs = result.consumeMulti(OutputItem.class);
        assertThat(outputs).extracting(OutputItem::getValue)
                .containsExactlyInAnyOrder("always:a", "always:b");
    }

    @Test
    void testBuildStepMethodOnlyIfNotTrue() throws Exception {
        BuildChainBuilder builder = BuildChain.builder();

        builder.addBuildStep(new BuildStep() {
            @Override
            public void execute(BuildContext bc) {
                bc.produce(new KeyedPropertyItem("a", "p1"));
                bc.produce(new KeyedPropertyItem("b", "p2"));
            }
        }).produces(KeyedPropertyItem.class).build();

        loadStepsWithConditions(MethodOnlyIfNotTrueProcessor.class).accept(builder);

        builder.addFinal(OutputItem.class);
        BuildResult result = builder.build().createExecutionBuilder("test").execute();

        List<OutputItem> outputs = result.consumeMulti(OutputItem.class);
        assertThat(outputs).extracting(OutputItem::getValue)
                .containsExactlyInAnyOrder("always:a", "always:b");
    }

    // -- exception collection tests --

    @Test
    void testMultipleKeyFailuresCollectSuppressed() throws Exception {
        BuildChainBuilder builder = BuildChain.builder();

        builder.addBuildStep(new BuildStep() {
            @Override
            public void execute(BuildContext bc) {
                bc.produce(new KeyedPropertyItem("a", "p1"));
                bc.produce(new KeyedPropertyItem("b", "p2"));
                bc.produce(new KeyedPropertyItem("c", "p3"));
            }
        }).produces(KeyedPropertyItem.class).build();

        loadSteps(FailingKeyedProcessor.class).accept(builder);

        builder.addFinal(OutputItem.class);
        BuildChain chain = builder.build();

        assertThatThrownBy(() -> chain.createExecutionBuilder("test").execute())
                .isInstanceOf(BuildException.class)
                .hasCauseInstanceOf(IllegalStateException.class)
                .satisfies(e -> {
                    Throwable cause = e.getCause();
                    assertThat(cause.getMessage()).contains("Failure for key:");
                    assertThat(cause.getSuppressed()).hasSize(2);
                    String allMessages = cause.getMessage();
                    for (Throwable suppressed : cause.getSuppressed()) {
                        allMessages += " " + suppressed.getMessage();
                    }
                    assertThat(allMessages).contains("a").contains("b").contains("c");
                });
    }

    @Test
    void testSingleKeyFailureNoSuppressed() throws Exception {
        BuildChainBuilder builder = BuildChain.builder();

        builder.addBuildStep(new BuildStep() {
            @Override
            public void execute(BuildContext bc) {
                bc.produce(new KeyedPropertyItem("only", "p1"));
            }
        }).produces(KeyedPropertyItem.class).build();

        loadSteps(FailingKeyedProcessor.class).accept(builder);

        builder.addFinal(OutputItem.class);
        BuildChain chain = builder.build();

        assertThatThrownBy(() -> chain.createExecutionBuilder("test").execute())
                .isInstanceOf(BuildException.class)
                .hasCauseInstanceOf(IllegalStateException.class)
                .satisfies(e -> {
                    Throwable cause = e.getCause();
                    assertThat(cause.getMessage()).isEqualTo("Failure for key: only");
                    assertThat(cause.getSuppressed()).isEmpty();
                });
    }

    public static final class DualKeyItem extends MultiBuildItem {
        @Key(TestDS.class)
        private final String dsName;
        @Key(OtherQualifier.class)
        private final String otherName;

        public DualKeyItem(String dsName, String otherName) {
            this.dsName = dsName;
            this.otherName = otherName;
        }
    }
}
