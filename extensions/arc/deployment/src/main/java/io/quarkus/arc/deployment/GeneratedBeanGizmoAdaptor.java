package io.quarkus.arc.deployment;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import io.quarkus.bootstrap.BootstrapDebug;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.gizmo.ClassOutput;

public class GeneratedBeanGizmoAdaptor implements ClassOutput {

    private final BuildProducer<GeneratedBeanBuildItem> classOutput;
    private final Map<String, StringWriter> sources;
    private final Predicate<String> applicationClassPredicate;

    public GeneratedBeanGizmoAdaptor(BuildProducer<GeneratedBeanBuildItem> classOutput) {
        this(classOutput, new Predicate<String>() {

            @Override
            public boolean test(String t) {
                return true;
            }
        });
    }

    public GeneratedBeanGizmoAdaptor(BuildProducer<GeneratedBeanBuildItem> classOutput,
            Predicate<String> applicationClassPredicate) {
        this.classOutput = classOutput;
        this.sources = BootstrapDebug.debugSourcesDir() != null ? new ConcurrentHashMap<>() : null;
        this.applicationClassPredicate = applicationClassPredicate;
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
        classOutput.produce(new GeneratedBeanBuildItem(className, bytes, source, applicationClassPredicate.test(className)));
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
