package org.jboss.resteasy.reactive.server.processor.scanning;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.server.core.parameters.ParameterExtractor;
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;

public interface MethodScanner {

    default List<HandlerChainCustomizer> scan(MethodInfo method, Map<String, Object> methodContext) {
        return Collections.emptyList();
    }

    default ParameterExtractor handleCustomParameter(Type paramType, Map<DotName, AnnotationInstance> annotations,
            boolean field, Map<String, Object> methodContext) {
        return null;
    }

}
