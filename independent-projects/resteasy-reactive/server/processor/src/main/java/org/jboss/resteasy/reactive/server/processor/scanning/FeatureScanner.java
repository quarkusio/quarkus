package org.jboss.resteasy.reactive.server.processor.scanning;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.jboss.jandex.IndexView;
import org.jboss.resteasy.reactive.server.processor.ScannedApplication;
import org.jboss.resteasy.reactive.server.processor.ServerEndpointIndexer;
import org.jboss.resteasy.reactive.server.processor.util.GeneratedClass;
import org.objectweb.asm.ClassVisitor;

/**
 * Integration point that can generate and transform classes
 */
public interface FeatureScanner {

    FeatureScanResult integrate(IndexView application, ScannedApplication scannedApplication);

    default void integrateWithIndexer(ServerEndpointIndexer.Builder builder, IndexView index) {

    }

    class FeatureScanResult {
        final List<GeneratedClass> generatedClasses;
        final Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> transformers;

        public FeatureScanResult(List<GeneratedClass> generatedClasses,
                Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> transformers) {
            this.generatedClasses = generatedClasses;
            this.transformers = transformers;
        }

        public FeatureScanResult(List<GeneratedClass> generatedClasses) {
            this.generatedClasses = generatedClasses;
            this.transformers = Collections.emptyMap();
        }

        public List<GeneratedClass> getGeneratedClasses() {
            return generatedClasses;
        }

        public Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> getTransformers() {
            return transformers;
        }
    }
}
