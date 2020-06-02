package io.quarkus.kubernetes.client.runtime.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.fabric8.kubernetes.api.model.KubernetesResource;

@TargetClass(className = "io.fabric8.kubernetes.internal.KubernetesDeserializer", innerClass = "Mapping")
public final class KubernetesDeserializerSubstitutions {

    /**
     * This method needs to be substituted because the original method calls
     *
     * <pre>
     * return KubernetesDeserializer.class.getClassLoader().loadClass(className);
     * </pre>
     *
     * which cannot work in GraalVM. Class.forName() however does work on GraalVM
     * and ultimately returns what we need in this case, which is the Class
     */
    @Substitute
    private Class<? extends KubernetesResource> loadClassIfExists(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            if (!KubernetesResource.class.isAssignableFrom(clazz)) {
                return null;
            }
            return (Class<? extends KubernetesResource>) clazz;
        } catch (Exception t) {
            return null;
        }
    }
}
