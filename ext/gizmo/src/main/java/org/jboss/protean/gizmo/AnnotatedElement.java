package org.jboss.protean.gizmo;

public interface AnnotatedElement {

    AnnotationCreator addAnnotation(String annotationType);

    AnnotationCreator addAnnotation(Class<?> annotationType);

}
