package io.quarkus.deployment.builditem;

import org.jboss.builder.item.MultiBuildItem;

/**
 * This is an optional build item that allows us to track annotations that will define test classes
 * It is only available during tests
 */
public final class TestAnnotationBuildItem extends MultiBuildItem {

    private final String annotationClassName;

    public TestAnnotationBuildItem(String annotationClassName) {
        this.annotationClassName = annotationClassName;
    }

    public String getAnnotationClassName() {
        return annotationClassName;
    }
}
