package io.quarkus.arc.impl;

import jakarta.enterprise.context.ApplicationScoped;
import java.lang.annotation.Annotation;

class ApplicationContext extends AbstractSharedContext {

    @Override
    public Class<? extends Annotation> getScope() {
        return ApplicationScoped.class;
    }
}
