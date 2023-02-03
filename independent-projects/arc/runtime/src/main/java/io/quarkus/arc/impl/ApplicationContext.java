package io.quarkus.arc.impl;

import java.lang.annotation.Annotation;

import jakarta.enterprise.context.ApplicationScoped;

class ApplicationContext extends AbstractSharedContext {

    @Override
    public Class<? extends Annotation> getScope() {
        return ApplicationScoped.class;
    }
}
