package io.quarkus.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Tests around build-step prioritization and multi-item ordering across multiple producers.
 */
class BuildStepPrioritizerTest {

    private static final class BaseConfigBuildItem extends SimpleBuildItem {
    }

    private static final class LoggingSetupBuildItem extends SimpleBuildItem {
    }

    private static final class FeatureMetadataBuildItem extends SimpleBuildItem {
    }

    private static final class FinalBundleBuildItem extends SimpleBuildItem {
    }

    private static final class GeneratedBytecodeBuildItem extends MultiBuildItem {
        private final String label;

        private GeneratedBytecodeBuildItem(String label) {
            this.label = label;
        }

        String label() {
            return label;
        }
    }

    private static final class FinalAssemblerStep implements BuildStep {

        private final List<String> consumedOrder;

        FinalAssemblerStep(List<String> consumedOrder) {
            this.consumedOrder = consumedOrder;
        }

        @Override
        public void execute(BuildContext context) {
            context.consume(LoggingSetupBuildItem.class);
            context.consume(FeatureMetadataBuildItem.class);
            context.consumeMulti(GeneratedBytecodeBuildItem.class).forEach(b -> consumedOrder.add(b.label()));
            context.produce(new FinalBundleBuildItem());
        }

        @Override
        public String getId() {
            return "final-assembler";
        }
    }

    private static final class BaseConfigStep implements BuildStep {

        @Override
        public void execute(BuildContext context) {
            context.produce(new BaseConfigBuildItem());
            context.produce(new GeneratedBytecodeBuildItem("bytecode-from-base-config"));
        }

        @Override
        public String getId() {
            return "base-config";
        }
    }

    private static final class LoggingSetupStep implements BuildStep {

        @Override
        public void execute(BuildContext context) {
            context.consume(BaseConfigBuildItem.class);
            context.produce(new LoggingSetupBuildItem());
            context.produce(new GeneratedBytecodeBuildItem("bytecode-from-logging"));
        }

        @Override
        public String getId() {
            return "logging-setup";
        }
    }

    private static final class FeatureInitStep implements BuildStep {

        @Override
        public void execute(BuildContext context) {
            context.produce(new GeneratedBytecodeBuildItem("bytecode-from-feature"));
            context.produce(new FeatureMetadataBuildItem());
        }

        @Override
        public String getId() {
            return "feature-init";
        }
    }

    BuildChainBuilder setupChainBuilder(List<String> consumedOrder) throws ChainBuildException, BuildException {
        // Arrange a chain with two producers for a multi item and a final consumer that observes the list order
        BuildChainBuilder chainBuilder = BuildChain.builder();

        BuildStepBuilder baseConfigStep = chainBuilder.addBuildStep(new BaseConfigStep());
        baseConfigStep.produces(BaseConfigBuildItem.class);
        baseConfigStep.produces(GeneratedBytecodeBuildItem.class);
        baseConfigStep.build();

        BuildStepBuilder loggingStep = chainBuilder.addBuildStep(new LoggingSetupStep());
        loggingStep.consumes(BaseConfigBuildItem.class);
        loggingStep.produces(LoggingSetupBuildItem.class);
        loggingStep.produces(GeneratedBytecodeBuildItem.class);
        loggingStep.build();

        BuildStepBuilder featureStep = chainBuilder.addBuildStep(new FeatureInitStep());
        featureStep.produces(GeneratedBytecodeBuildItem.class);
        featureStep.produces(FeatureMetadataBuildItem.class);
        featureStep.build();

        BuildStepBuilder finalStep = chainBuilder.addBuildStep(new FinalAssemblerStep(consumedOrder));
        finalStep.consumes(LoggingSetupBuildItem.class);
        finalStep.consumes(GeneratedBytecodeBuildItem.class);
        finalStep.consumes(FeatureMetadataBuildItem.class);
        finalStep.produces(FinalBundleBuildItem.class);
        finalStep.build();

        chainBuilder.addFinal(FinalBundleBuildItem.class);

        return chainBuilder;
    }

    @Test
    void consumesMultiBuildItemsInProducingOrdinalOrder() throws ChainBuildException, BuildException {
        List<String> consumedOrder = new ArrayList<>();
        BuildChainBuilder chainBuilder = setupChainBuilder(consumedOrder);
        chainBuilder.build().createExecutionBuilder("ordering-test").execute();
        // Without prioritization, producing ordinal favors feature-init before other steps
        assertEquals(List.of("bytecode-from-feature", "bytecode-from-base-config", "bytecode-from-logging"), consumedOrder);
    }

    @Test
    void consumesMultiBuildItemsInProducingOrdinalOrder2() throws ChainBuildException, BuildException {
        List<String> consumedOrder = new ArrayList<>();
        BuildChainBuilder chainBuilder = setupChainBuilder(consumedOrder);
        chainBuilder.addPriorityItem(LoggingSetupBuildItem.class);
        chainBuilder.build().createExecutionBuilder("ordering-test").execute();
        // With logging prioritized, config bytecode comes first (as a dependency of logging), then logging, then feature
        assertEquals(List.of("bytecode-from-base-config", "bytecode-from-logging", "bytecode-from-feature"), consumedOrder);
    }

}
