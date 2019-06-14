package io.quarkus.arc;

import java.lang.annotation.Annotation;
import javax.inject.Singleton;

class SingletonContext extends AbstractSharedContext {

    @Override
    public Class<? extends Annotation> getScope() {
        return Singleton.class;
    }

}
