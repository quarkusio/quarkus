package io.quarkus.arc;

import java.lang.annotation.Annotation;
import javax.enterprise.context.ApplicationScoped;

class ApplicationContext extends AbstractSharedContext {

    @Override
    public Class<? extends Annotation> getScope() {
        return ApplicationScoped.class;
    }
}
