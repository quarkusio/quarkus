package org.jboss.resteasy.reactive.common.processor;

import org.jboss.jandex.ClassInfo;

public interface EndpointAnnotationHandler {

    boolean isEndpointAnnotationValid(ClassInfo classInfo);

}
