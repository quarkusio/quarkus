package io.quarkus.rest.client.reactive.runtime;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;

@SuppressWarnings("unused")
public class BeanGrabber {
    public static <T> T getBeanIfDefined(Class<T> beanClass) {
        InstanceHandle<T> instance = Arc.container().instance(beanClass);
        if (instance.isAvailable()) {
            return instance.get();
        }
        return null;
    }

    private BeanGrabber() {
    }
}
