package io.quarkus.arc.impl;

import io.quarkus.arc.ContextInstanceHandle;
import io.quarkus.arc.InstanceHandle;
import jakarta.inject.Singleton;
import java.lang.annotation.Annotation;

class SingletonContext extends AbstractSharedContext {

    @Override
    public Class<? extends Annotation> getScope() {
        return Singleton.class;
    }

    void destroyInstance(Object instance) {
        InstanceHandle<?> handle = null;
        for (ContextInstanceHandle<?> contextInstance : instances.getPresentValues()) {
            if (contextInstance.get() == instance) {
                handle = contextInstance;
                break;
            }
        }
        if (handle != null) {
            handle = instances.remove(handle.getBean().getIdentifier());
            if (handle != null) {
                handle.destroy();
            }
        }
    }

}
