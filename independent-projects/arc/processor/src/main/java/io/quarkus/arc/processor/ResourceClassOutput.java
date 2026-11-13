package io.quarkus.arc.processor;

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.quarkus.arc.processor.ResourceOutput.Resource;
import io.quarkus.arc.processor.ResourceOutput.Resource.BootstrapMethod;
import io.quarkus.arc.processor.ResourceOutput.Resource.SpecialType;
import io.quarkus.gizmo2.ClassOutput;

public class ResourceClassOutput implements ClassOutput {

    private static final Function<String, SpecialType> NO_SPECIAL_TYPE = cn -> null;

    private final List<Resource> resources = new ArrayList<>();
    // bootstrap methods are registered while a class is being built, before it is written
    private final Map<String, List<BootstrapMethod>> bootstrapMethods = new HashMap<>();
    private final boolean applicationClass;
    private final Function<String, SpecialType> specialTypeFunction;

    // note that the `generateSource` parameters are unused for now,
    // until https://github.com/quarkusio/gizmo/issues/419 is implemented

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
    }

    @Override
    public void write(String name, byte[] data) {
        if (name.endsWith(".class")) {
            // Gizmo 2 calls `write()` with the full file path
            name = name.substring(0, name.length() - ".class".length());
        }
        String className = name.replace('/', '.');
        List<BootstrapMethod> classBootstrapMethods = bootstrapMethods.remove(className);
        resources.add(ResourceImpl.javaClass(name, data, specialTypeFunction.apply(className),
                applicationClass, null, classBootstrapMethods == null ? List.of() : classBootstrapMethods));
    }

    @Override
    public void registerBootstrapMethod(ClassDesc owner, String name, MethodTypeDesc type) {
        // `Lcom/foo/Bar;` -> `com.foo.Bar`
        String descriptor = owner.descriptorString();
        String className = descriptor.substring(1, descriptor.length() - 1).replace('/', '.');
        bootstrapMethods.computeIfAbsent(className, k -> new ArrayList<>()).add(new BootstrapMethod(name, type));
    }

    List<Resource> getResources() {
        return resources;
    }
}
