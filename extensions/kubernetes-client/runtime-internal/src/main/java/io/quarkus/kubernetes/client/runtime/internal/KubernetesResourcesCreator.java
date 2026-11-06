package io.quarkus.kubernetes.client.runtime.internal;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;

public class KubernetesResourcesCreator implements BeanCreator<Class<?>[]> {

    public static final String RESOURCE_CLASSES_PARAM = "resourceClasses";

    @Override
    public Class<?>[] create(SyntheticCreationalContext<Class<?>[]> context) {
        return (Class<?>[]) context.getParams().get(RESOURCE_CLASSES_PARAM);
    }
}
