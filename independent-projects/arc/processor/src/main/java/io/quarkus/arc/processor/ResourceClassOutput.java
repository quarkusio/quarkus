package io.quarkus.arc.processor;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.quarkus.arc.processor.ResourceOutput.Resource;
import io.quarkus.arc.processor.ResourceOutput.Resource.SpecialType;

public class ResourceClassOutput implements io.quarkus.gizmo.ClassOutput, io.quarkus.gizmo2.ClassOutput {

    private static final Function<String, SpecialType> NO_SPECIAL_TYPE = cn -> null;

    private final List<Resource> resources = new ArrayList<>();
    private final Map<String, StringWriter> sources;
    private final boolean applicationClass;
    private final Function<String, SpecialType> specialTypeFunction;

    /**
     * @param applicationClass whether the generated classes are application classes or not
     * @param generateSource whether to also generate textual representation of the code
     */
    public ResourceClassOutput(boolean applicationClass, boolean generateSource) {
        this(applicationClass, NO_SPECIAL_TYPE, generateSource);
    }

    /**
     * @param applicationClass whether the generated classes are application classes or not
     * @param specialTypeFunction function accepting a binary name of the generated class and returning
     *        the {@link SpecialType} of the class (or {@code null})
     * @param generateSource whether to also generate textual representation of the code
     */
    public ResourceClassOutput(boolean applicationClass, Function<String, SpecialType> specialTypeFunction,
            boolean generateSource) {
        this.applicationClass = applicationClass;
        this.specialTypeFunction = specialTypeFunction;
        // Note that ResourceClassOutput is never used concurrently
        this.sources = generateSource ? new HashMap<>() : null;
    }

    @Override
    public void write(String name, byte[] data) {
        if (name.endsWith(".class")) {
            // Gizmo 2 calls `write()` with the full file path
            name = name.substring(0, name.length() - ".class".length());
        }
        String className = name.replace('/', '.');
        resources.add(ResourceImpl.javaClass(name, data, specialTypeFunction.apply(className),
                applicationClass, getSource(name)));
    }

    @Override
    public Writer getSourceWriter(String className) {
        if (sources != null) {
            StringWriter writer = new StringWriter();
            sources.put(className, writer);
            return writer;
        }
        return io.quarkus.gizmo.ClassOutput.super.getSourceWriter(className);
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
