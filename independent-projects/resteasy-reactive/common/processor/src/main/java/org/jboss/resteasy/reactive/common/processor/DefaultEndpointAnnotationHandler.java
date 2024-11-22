package org.jboss.resteasy.reactive.common.processor;

import java.lang.reflect.Modifier;

import org.jboss.jandex.ClassInfo;

public class DefaultEndpointAnnotationHandler implements EndpointAnnotationHandler {
    public DefaultEndpointAnnotationHandler() {
    }

    @Override
    public boolean isEndpointAnnotationValid(ClassInfo classInfo) {
        if (Modifier.isInterface(classInfo.flags()) || Modifier.isAbstract(classInfo.flags())) {
            return true;
        }
        return false;
    }
}
