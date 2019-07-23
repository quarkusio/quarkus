package io.quarkus.arc.processor;

import java.util.Collection;
import org.jboss.jandex.DotName;

/**
 * Allows to programatically register additional interceptor bindings.
 */
public interface InterceptorBindingRegistrar extends BuildExtension {

    /**
     * Returns a collection of annotations in a form of {@link DotName} that are to be considered interceptor bindings.
     */
    Collection<DotName> registerAdditionalBindings();
}