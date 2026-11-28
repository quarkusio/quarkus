package io.quarkus.bootstrap.model.gradle.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import io.quarkus.bootstrap.model.gradle.GradleLogicalOutput;
import io.quarkus.bootstrap.model.gradle.GradleProjectComponent;
import io.quarkus.bootstrap.model.gradle.GradleProjectIdentity;
import io.quarkus.bootstrap.model.gradle.GradleSourceObservation;

public final class DefaultGradleProjectComponent implements GradleProjectComponent {

    private static final long serialVersionUID = -5391398664530192947L;

    private final GradleProjectIdentity projectIdentity;
    private final String selectedComponentIdentity;
    private final Set<Role> roles;
    private final Set<ClasspathAssociation> classpathAssociations;
    private final Set<GraphRelationship> graphRelationships;
    private final List<? extends GradleSourceObservation> sourceObservations;
    private final List<? extends GradleLogicalOutput> logicalOutputs;

    public DefaultGradleProjectComponent(GradleProjectIdentity projectIdentity, String selectedComponentIdentity,
            Collection<Role> roles, Collection<ClasspathAssociation> classpathAssociations,
            Collection<GraphRelationship> graphRelationships,
            Collection<? extends GradleSourceObservation> sourceObservations,
            Collection<? extends GradleLogicalOutput> logicalOutputs) {
        this.projectIdentity = Objects.requireNonNull(projectIdentity, "projectIdentity");
        this.selectedComponentIdentity = Objects.requireNonNull(selectedComponentIdentity, "selectedComponentIdentity");
        this.roles = immutableEnumSet(roles);
        this.classpathAssociations = immutableEnumSet(classpathAssociations);
        this.graphRelationships = immutableEnumSet(graphRelationships);
        this.sourceObservations = List.copyOf(sourceObservations);
        this.logicalOutputs = List.copyOf(logicalOutputs);
    }

    @Override
    public GradleProjectIdentity getProjectIdentity() {
        return projectIdentity;
    }

    @Override
    public String getSelectedComponentIdentity() {
        return selectedComponentIdentity;
    }

    @Override
    public Set<Role> getRoles() {
        return roles;
    }

    @Override
    public Set<ClasspathAssociation> getClasspathAssociations() {
        return classpathAssociations;
    }

    @Override
    public Set<GraphRelationship> getGraphRelationships() {
        return graphRelationships;
    }

    @Override
    public List<? extends GradleSourceObservation> getSourceObservations() {
        return sourceObservations;
    }

    @Override
    public List<? extends GradleLogicalOutput> getLogicalOutputs() {
        return logicalOutputs;
    }

    private static <E extends Enum<E>> Set<E> immutableEnumSet(Collection<E> values) {
        Objects.requireNonNull(values, "values");
        if (values.isEmpty()) {
            return Set.of();
        }
        return Collections.unmodifiableSet(EnumSet.copyOf(values));
    }
}
