package io.quarkus.deployment.steps;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

import org.jboss.logging.Logger;

import io.quarkus.deployment.GeneratedClassGizmo2Adaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.GeneratedServiceProviderBuildItem;
import io.quarkus.deployment.builditem.MaterializeLambdasBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ConstantBootstrapBuildItem;
import io.quarkus.deployment.lambda.LambdaMaterializer;
import io.quarkus.deployment.util.IoUtil;
import io.quarkus.gizmo2.ClassOutput;

/**
 * Rewrites the lambdas of the classes listed by {@link MaterializeLambdasBuildItem}s into generated classes.
 */
public class LambdaMaterializationBuildStep {

    private static final Logger log = Logger.getLogger(LambdaMaterializationBuildStep.class);
    private static final ClassOutput NO_OUTPUT = (name, bytes) -> {
    };

    @BuildStep
    void materializeLambdas(List<MaterializeLambdasBuildItem> classes,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<GeneratedResourceBuildItem> generatedResources,
            BuildProducer<GeneratedServiceProviderBuildItem> generatedServiceProviders,
            BuildProducer<ConstantBootstrapBuildItem> constantBootstraps) throws IOException {
        ClassOutput output = new GeneratedClassGizmo2Adaptor(generatedClasses, generatedResources,
                generatedServiceProviders, constantBootstraps, false);
        Set<String> seen = new HashSet<>();
        for (MaterializeLambdasBuildItem item : classes) {
            String className = item.getClassName();
            if (!seen.add(className)) {
                continue;
            }
            byte[] bytes = IoUtil.readClassAsBytes(Thread.currentThread().getContextClassLoader(), className);
            if (bytes == null) {
                throw new IllegalArgumentException("Cannot rewrite the lambdas of " + className + ": class not found");
            }
            // the generated classes are produced now, from the class as it is on the class path; the host class is
            // rewritten when the transformers run, from the bytes it has by then, which generate the same classes
            LambdaMaterializer.Result result = LambdaMaterializer.materialize(bytes, output);
            if (result.callSites() == 0) {
                log.debugf("No lambda to rewrite in %s", className);
                continue;
            }
            transformers.produce(new BytecodeTransformerBuildItem.Builder()
                    .setClassToTransform(className)
                    .setCacheable(true)
                    .setInputTransformer(new BiFunction<String, byte[], byte[]>() {
                        @Override
                        public byte[] apply(String name, byte[] input) {
                            // the generated classes were produced above
                            return LambdaMaterializer.materialize(input, NO_OUTPUT).hostClass();
                        }
                    })
                    .build());
            log.debugf("Rewrote %d lambda call sites of %s", result.callSites(), className);
        }
    }
}
