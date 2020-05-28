package io.quarkus.deployment;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.quarkus.bootstrap.BootstrapDebug;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.gizmo.ClassOutput;

public class GeneratedClassGizmoAdaptor implements ClassOutput {

    private final BuildProducer<GeneratedClassBuildItem> classOutput;
    private final boolean applicationClass;
    private final Map<String, StringWriter> sources;

    public GeneratedClassGizmoAdaptor(BuildProducer<GeneratedClassBuildItem> classOutput, boolean applicationClass) {
        this.classOutput = classOutput;
        this.applicationClass = applicationClass;
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
        classOutput.produce(
                new GeneratedClassBuildItem(applicationClass, className, bytes, source));
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
