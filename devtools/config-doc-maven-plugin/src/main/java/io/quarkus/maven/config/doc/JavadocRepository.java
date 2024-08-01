package io.quarkus.maven.config.doc;

import java.util.Map;
import java.util.Optional;

import io.quarkus.annotation.processor.documentation.config.model.JavadocElements.JavadocElement;
import io.quarkus.annotation.processor.documentation.config.util.Markers;

final class JavadocRepository {

    private final Map<String, JavadocElement> javadocElementsMap;

    JavadocRepository(Map<String, JavadocElement> javadocElementsMap) {
        this.javadocElementsMap = javadocElementsMap;
    }

    public Optional<JavadocElement> getElement(String className, String elementName) {
        return Optional.ofNullable(javadocElementsMap.get(className + Markers.DOT + elementName));
    }
}
