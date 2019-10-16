package io.quarkus.arc.processor;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.jboss.jandex.DotName;

public interface InterceptorBindingRegistrar extends BuildExtension {

    /**
     * Annotations in a form of {@link DotName} to be considered interceptor bindings.
     * Optionally, mapped to a {@link Collection} of non-binding fields
     */
    Map<DotName, Set<String>> registerAdditionalBindings();
}
