package io.quarkus.kubernetes.client.runtime.internal;

import java.util.HashSet;
import java.util.Set;

import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class KubernetesSerializationRecorder {

    public RuntimeValue<Class<? extends KubernetesResource>[]> initKubernetesResources(String[] resourceClassNames) {
        final Set<Class<?>> resourceClasses = new HashSet<>();
        for (var resourceClassName : resourceClassNames) {
            try {
                resourceClasses.add(
                        Class.forName(resourceClassName, false, KubernetesSerializationRecorder.class.getClassLoader()));
            } catch (ClassNotFoundException e) {
            }
        }
        return new RuntimeValue<>(resourceClasses.toArray(Class[]::new));
    }
}
