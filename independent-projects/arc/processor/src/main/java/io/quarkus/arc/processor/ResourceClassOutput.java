package io.quarkus.arc.processor;

import io.quarkus.arc.processor.ResourceOutput.Resource;
import io.quarkus.arc.processor.ResourceOutput.Resource.SpecialType;
import io.quarkus.gizmo.ClassOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.jboss.jandex.DotName;

/**
 *
 * @author Martin Kouba
 */
public class ResourceClassOutput implements ClassOutput {

    private final List<Resource> resources = new ArrayList<>();

    private final boolean applicationClass;
    private final DotName associatedClassName;

    private final Function<String, SpecialType> specialTypeFunction;

    public ResourceClassOutput(boolean applicationClass, DotName associatedClassName) {
        this(applicationClass, associatedClassName, null);
    }

    public ResourceClassOutput(boolean applicationClass, DotName associatedClassName,
            Function<String, SpecialType> specialTypeFunction) {
        this.applicationClass = applicationClass;
        this.associatedClassName = associatedClassName;
        this.specialTypeFunction = specialTypeFunction;
    }

    @Override
    public void write(String name, byte[] data) {
        resources.add(ResourceImpl.javaClass(name, data, specialTypeFunction != null ? specialTypeFunction.apply(name) : null,
                applicationClass, associatedClassName));
    }

    List<Resource> getResources() {
        return resources;
    }

}
