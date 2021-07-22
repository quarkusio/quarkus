package org.jboss.resteasy.reactive.server.processor.scanning;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.server.core.parameters.ParameterExtractor;
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;

/**
 * SPI that allows external integrations to handle custom parameters.
 */
public interface MethodScanner {

    /**
     * Method that allows for customising an endpoints handler chain
     * 
     * @param method The method
     * @param actualEndpointClass
     * @param methodContext Any context discovered by {@link #handleCustomParameter(Type, Map, boolean, Map)}
     * @return
     */
    default List<HandlerChainCustomizer> scan(MethodInfo method, ClassInfo actualEndpointClass,
            Map<String, Object> methodContext) {
        return Collections.emptyList();
    }

    /**
     * Method that is called when a parameter of an unknown type is discovered.
     * 
     * @param paramType The parameter type
     * @param annotations The annotations
     * @param field If this is field injection
     * @param methodContext Context that can be used to pass information into {@link #scan(MethodInfo, ClassInfo, Map)}
     * @return
     */
    default ParameterExtractor handleCustomParameter(Type paramType, Map<DotName, AnnotationInstance> annotations,
            boolean field, Map<String, Object> methodContext) {
        return null;
    }

    default boolean isMethodSignatureAsync(MethodInfo info) {
        return false;
    }

}
