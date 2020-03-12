package io.quarkus.arc.processor;

import io.quarkus.arc.processor.ResourceOutput.Resource;
import io.quarkus.arc.processor.ResourceOutput.Resource.SpecialType;
import io.quarkus.gizmo.ClassOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 *
 * @author Martin Kouba
 */
public class ResourceClassOutput implements ClassOutput {

    private final List<Resource> resources = new ArrayList<>();

    private final boolean applicationClass;

    private final Function<String, SpecialType> specialTypeFunction;

    public ResourceClassOutput(boolean applicationClass) {
        this(applicationClass, null);
    }

    public ResourceClassOutput(boolean applicationClass, Function<String, SpecialType> specialTypeFunction) {
        this.applicationClass = applicationClass;
        this.specialTypeFunction = specialTypeFunction;
    }

    @Override
    public void write(String name, byte[] data) {
        resources.add(ResourceImpl.javaClass(name, data, specialTypeFunction != null ? specialTypeFunction.apply(name) : null,
                applicationClass));
    }

    List<Resource> getResources() {
        return resources;
    }

}
