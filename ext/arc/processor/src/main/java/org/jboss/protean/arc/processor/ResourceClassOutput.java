package org.jboss.protean.arc.processor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.jboss.protean.arc.processor.ResourceOutput.Resource;
import org.jboss.protean.arc.processor.ResourceOutput.Resource.SpecialType;
import org.jboss.protean.gizmo.ClassOutput;

/**
 *
 * @author Martin Kouba
 */
public class ResourceClassOutput implements ClassOutput {

    private final List<Resource> resources = new ArrayList<>();

    private final Function<String, SpecialType> specialTypeFunction;

    public ResourceClassOutput() {
        this(null);
    }

    public ResourceClassOutput(Function<String, SpecialType> specialTypeFunction) {
        this.specialTypeFunction = specialTypeFunction;
    }

    @Override
    public void write(String name, byte[] data) {
        resources.add(ResourceImpl.javaClass(name, data, specialTypeFunction != null ? specialTypeFunction.apply(name) : null));
    }

    List<Resource> getResources() {
        return resources;
    }

}
