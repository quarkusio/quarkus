package io.quarkiverse.acme.deployment;

import org.jboss.jandex.DotName;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class ExtensionProcessor {

    private static final String FEATURE = "extension-that-has-capability-for-tests";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    BytecodeTransformerBuildItem reworkClassLoadingOfParameterizedSourceTest2() {
        // Ideally, we would not hardcode class names, but this is a reproducer so we can shortcut
        DotName simple = DotName.createSimple("org.acme.ParameterizedTest");
        return new BytecodeTransformerBuildItem.Builder()
                .setClassToTransform(simple.toString())
                .setVisitorFunction((ignored, visitor) -> new AnnotationAdjuster(visitor,
                        simple.toString()))
                .build();

    }

    @BuildStep
    BytecodeTransformerBuildItem reworkClassLoadingOfParameterizedSourceQuarkusTest() {
        // Ideally, we would not hardcode class names, but this is a reproducer so we can shortcut
        DotName simple = DotName.createSimple("org.acme.ParameterizedQuarkusTest");
        return new BytecodeTransformerBuildItem.Builder()
                .setClassToTransform(simple.toString())
                .setVisitorFunction((ignored, visitor) -> new AnnotationAdjuster(visitor,
                        simple.toString()))
                .build();
    }

    @BuildStep
    BytecodeTransformerBuildItem reworkClassLoadingOfNormalSourceQuarkusTest() {
        // Ideally, we would not hardcode class names, but this is a reproducer so we can shortcut
        DotName simple = DotName.createSimple("org.acme.NormalQuarkusTest");
        return new BytecodeTransformerBuildItem.Builder()
                .setClassToTransform(simple.toString())
                .setVisitorFunction((ignored, visitor) -> new AnnotationAdjuster(visitor,
                        simple.toString()))
                .build();
    }
}
