package io.quarkus.spring.web.deployment;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.resteasy.reactive.common.processor.EndpointAnnotationHandler;

public class SpringRestControllerAnnotationHandler implements EndpointAnnotationHandler {
    public static final DotName SPRING_REST_CONTROLLER = DotName
            .createSimple("org.springframework.web.bind.annotation.RestController");

    @Override
    public boolean isEndpointAnnotationValid(ClassInfo classInfo) {
        return classInfo
                .declaredAnnotation(SPRING_REST_CONTROLLER) != null;
    }

}
