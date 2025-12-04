package io.quarkus.qute.deployment;

import java.net.URI;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;

import io.quarkus.qute.JavaElementUriBuilder;

/**
 * Builder for Qute-specific URIs that reference a Jandex Java element
 * (class, method, or annotation) from a template.
 * <p>
 * These URIs have the format:
 *
 * <pre>
 * qute-java://&lt;fully-qualified-class-name&gt;[#method][@annotation]
 * </pre>
 *
 * Examples:
 * <ul>
 * <li>Class-level annotation: <code>qute-java://com.acme.Bean@io.quarkus.qute.TemplateContents</code></li>
 * <li>Method-level annotation: <code>qute-java://com.acme.Bean#process@io.quarkus.qute.TemplateContents</code></li>
 * </ul>
 * </p>
 *
 * <p>
 * This builder is used to construct such URIs in a type-safe way and to provide
 * utility methods to identify and parse them. It is aligned with
 * {@link io.quarkus.qute.debug.client.JavaSourceLocationArguments#javaElementUri}.
 * </p>
 */
class JandexElementUriBuilder {

    private JandexElementUriBuilder() {

    }

    public static URI getSource(ClassInfo target, Class<?> annotationClass) {
        return JavaElementUriBuilder
                .builder(target.toString())
                .setAnnotation(annotationClass.getName())
                .build();
    }

    public static URI getSource(MethodInfo method, Class<?> annotationClass) {
        return JavaElementUriBuilder
                .builder(method.declaringClass().toString())
                .setMethod(method.name())
                .setAnnotation(annotationClass.getName())
                .build();
    }
}
