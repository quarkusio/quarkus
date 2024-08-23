package org.acme;

import java.lang.annotation.Annotation;
import java.util.Arrays;

public class AnnotationCountingTestCase {
    public AnnotationCountingTestCase(Annotation[] annotations, ClassLoader classloader) {
        this.annotations = annotations;
        this.classloader = classloader;
        displayString = Arrays.toString(annotations);
    }

    public String getDisplayString() {
        return displayString;
    }

    public Annotation[] getAnnotations() {
        return annotations;
    }

    private final ClassLoader classloader;
    private String displayString;
    private Annotation[] annotations;

    public ClassLoader getClassloader() {
        return classloader;
    }
}