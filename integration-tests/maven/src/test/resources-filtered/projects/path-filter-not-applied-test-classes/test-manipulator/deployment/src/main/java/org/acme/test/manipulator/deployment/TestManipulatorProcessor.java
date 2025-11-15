package org.acme.test.manipulator.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.gizmo.ClassTransformer;

/**
 * This processor simply adds a useless field to the test class just to get it transformed.
 * Having the test class transformed will put it in the Memory
 */
class TestManipulatorProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem("test-manipulator");
    }

    @BuildStep
    BytecodeTransformerBuildItem interceptTestMethods() {
        return new BytecodeTransformerBuildItem("org.acme.GreetingResourceTest", (cls, clsVisitor) -> {
            var transformer = new ClassTransformer(cls);
            transformer.addField("useless", String.class);
            return transformer.applyTo(clsVisitor);
        });
    }
}
