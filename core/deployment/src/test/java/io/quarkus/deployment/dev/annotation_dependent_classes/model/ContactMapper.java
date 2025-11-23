package io.quarkus.deployment.dev.annotation_dependent_classes.model;

@APMarker
public interface ContactMapper {
    void mapToData(Address contact);
}
