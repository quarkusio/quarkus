package org.jboss.resteasy.reactive.server.processor.generation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.jboss.jandex.IndexView;
import org.jboss.resteasy.reactive.server.processor.ScannedApplication;
import org.jboss.resteasy.reactive.server.processor.scanning.FeatureScanner;
import org.jboss.resteasy.reactive.server.processor.util.GeneratedClassOutput;
import org.objectweb.asm.ClassVisitor;

public class AbstractFeatureScanner implements FeatureScanner {

    protected final GeneratedClassOutput classOutput = new GeneratedClassOutput();
    protected final Map<String, BiFunction<String, ClassVisitor, ClassVisitor>> transformations = new HashMap<>();

    @Override
    public final FeatureScanResult integrate(IndexView application, ScannedApplication scannedApplication) {
        integrateImpl();
        Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> transformers = new HashMap<>();
        for (var e : transformations.entrySet()) {
            transformers.put(e.getKey(), List.of(e.getValue()));
        }
        return new FeatureScanResult(classOutput.getOutput(), transformers);
    }

    public void integrateImpl() {

    }

}
