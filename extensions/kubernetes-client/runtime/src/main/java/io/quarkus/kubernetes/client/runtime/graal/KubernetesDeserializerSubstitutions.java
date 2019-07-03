package io.quarkus.kubernetes.client.runtime.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "io.fabric8.kubernetes.internal.KubernetesDeserializer")
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
    private static Class loadClassIfExists(String className) {
        try {
            return Class.forName(className);
        } catch (Throwable t) {
            return null;
        }
    }
}
