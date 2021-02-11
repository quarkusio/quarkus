package io.quarkus.deployment;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import io.quarkus.bootstrap.BootstrapDebug;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.gizmo.ClassOutput;

public class GeneratedClassGizmoAdaptor implements ClassOutput {

    private final BuildProducer<GeneratedClassBuildItem> generatedClasses;
    private final Predicate<String> applicationClassPredicate;
    private final Map<String, StringWriter> sources;

    public GeneratedClassGizmoAdaptor(BuildProducer<GeneratedClassBuildItem> generatedClasses, boolean applicationClass) {
        this(generatedClasses, new Predicate<String>() {
            @Override
            public boolean test(String t) {
                return applicationClass;
            }
        });
    }

    public GeneratedClassGizmoAdaptor(BuildProducer<GeneratedClassBuildItem> generatedClasses,
            Predicate<String> applicationClassPredicate) {
        this.generatedClasses = generatedClasses;
        this.applicationClassPredicate = applicationClassPredicate;
        this.sources = BootstrapDebug.DEBUG_SOURCES_DIR != null ? new ConcurrentHashMap<>() : null;
    }

    @Override
    public void write(String className, byte[] bytes) {
        String source = null;
        if (sources != null) {
            StringWriter sw = sources.get(className);
            if (sw != null) {
                source = sw.toString();
            }
        }
        generatedClasses.produce(
                new GeneratedClassBuildItem(applicationClassPredicate.test(className), className, bytes, source));
    }

    @Override
    public Writer getSourceWriter(String className) {
        if (sources != null) {
            StringWriter writer = new StringWriter();
            sources.put(className, writer);
            return writer;
        }
        return ClassOutput.super.getSourceWriter(className);
    }

}
