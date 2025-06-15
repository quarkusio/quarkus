package io.quarkus.kubernetes.client;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

/**
 * {@link Qualifier} to inject the Fabric8 Kubernetes Client specific
 * {@link com.fasterxml.jackson.databind.ObjectMapper}.
 * <p>
 * Allows users to modify the behavior of the mapper for very specific use cases (such as adding Kotlin-specific
 * modules). Otherwise, it's not recommended to modify the mapper since it might break the Kubernetes Client.
 */
@Qualifier
@Retention(RUNTIME)
@Target({ METHOD, FIELD, PARAMETER, TYPE })
@Documented
public @interface KubernetesClientObjectMapper {

    final class Literal extends AnnotationLiteral<KubernetesClientObjectMapper>
            implements KubernetesClientObjectMapper {

        @SuppressWarnings("unused")
        public static final Literal INSTANCE = new Literal();
        private static final long serialVersionUID = 1L;

        private Literal() {
        }
    }
}
