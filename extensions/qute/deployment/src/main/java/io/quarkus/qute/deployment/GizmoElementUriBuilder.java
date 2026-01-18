package io.quarkus.qute.deployment;

import java.net.URI;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;

import io.quarkus.qute.JavaElementUriBuilder;

public class GizmoElementUriBuilder {

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
