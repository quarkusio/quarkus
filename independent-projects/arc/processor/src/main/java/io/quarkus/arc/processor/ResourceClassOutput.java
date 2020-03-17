package io.quarkus.arc.processor;

import io.quarkus.arc.processor.ResourceOutput.Resource;
import io.quarkus.arc.processor.ResourceOutput.Resource.SpecialType;
import io.quarkus.gizmo.ClassOutput;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 *
 */
public class ResourceClassOutput implements ClassOutput {

    private static final Function<String, SpecialType> NO_SPECIAL_TYPE = cn -> null;

    private final List<Resource> resources = new ArrayList<>();
    private final Map<String, StringWriter> sources;
    private final boolean applicationClass;
    private final Function<String, SpecialType> specialTypeFunction;

    public ResourceClassOutput(boolean applicationClass, boolean generateSource) {
        this(applicationClass, NO_SPECIAL_TYPE, generateSource);
    }

    public ResourceClassOutput(boolean applicationClass, Function<String, SpecialType> specialTypeFunction,
            boolean generateSource) {
        this.applicationClass = applicationClass;
        this.specialTypeFunction = specialTypeFunction;
        // Note that ResourceClassOutput is never used concurrently
        this.sources = generateSource ? new HashMap<>() : null;
    }

    @Override
    public void write(String name, byte[] data) {
        resources.add(ResourceImpl.javaClass(name, data, specialTypeFunction.apply(name),
                applicationClass, getSource(name)));
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

    List<Resource> getResources() {
        return resources;
    }

    String getSource(String className) {
        if (sources == null) {
            return null;
        }
        StringWriter source = sources.get(className);
        return source != null ? source.toString() : null;
    }

}
