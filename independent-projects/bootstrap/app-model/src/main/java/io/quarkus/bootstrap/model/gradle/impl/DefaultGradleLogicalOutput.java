package io.quarkus.bootstrap.model.gradle.impl;

import java.util.Objects;

import io.quarkus.bootstrap.model.gradle.GradleLogicalOutput;
import io.quarkus.bootstrap.model.gradle.GradleModelAssociation;

public final class DefaultGradleLogicalOutput implements GradleLogicalOutput {

    private static final long serialVersionUID = -7112065215159628735L;

    private final String identity;
    private final Kind kind;
    private final Scope scope;
    private final String path;
    private final String selectedArtifactIdentity;
    private final String producerSemanticIdentity;
    private final String sourceSet;
    private final String jvmFeature;
    private final String classifier;
    private final Materialization materialization;
    private final ProducerCategory producerCategory;
    private final GradleModelAssociation modelAssociation;

    public DefaultGradleLogicalOutput(String identity, Kind kind, Scope scope, String path,
            String selectedArtifactIdentity, String producerSemanticIdentity, String sourceSet, String jvmFeature,
            String classifier, Materialization materialization, ProducerCategory producerCategory,
            GradleModelAssociation modelAssociation) {
        this.identity = Objects.requireNonNull(identity, "identity");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.scope = Objects.requireNonNull(scope, "scope");
        this.path = Objects.requireNonNull(path, "path");
        this.selectedArtifactIdentity = Objects.requireNonNull(selectedArtifactIdentity, "selectedArtifactIdentity");
        this.producerSemanticIdentity = producerSemanticIdentity;
        this.sourceSet = sourceSet;
        this.jvmFeature = jvmFeature;
        this.classifier = classifier;
        this.materialization = Objects.requireNonNull(materialization, "materialization");
        this.producerCategory = Objects.requireNonNull(producerCategory, "producerCategory");
        this.modelAssociation = Objects.requireNonNull(modelAssociation, "modelAssociation");
    }

    @Override
    public String getIdentity() {
        return identity;
    }

    @Override
    public Kind getKind() {
        return kind;
    }

    @Override
    public Scope getScope() {
        return scope;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getSelectedArtifactIdentity() {
        return selectedArtifactIdentity;
    }

    @Override
    public String getProducerSemanticIdentity() {
        return producerSemanticIdentity;
    }

    @Override
    public String getSourceSet() {
        return sourceSet;
    }

    @Override
    public String getJvmFeature() {
        return jvmFeature;
    }

    @Override
    public String getClassifier() {
        return classifier;
    }

    @Override
    public Materialization getMaterialization() {
        return materialization;
    }

    @Override
    public ProducerCategory getProducerCategory() {
        return producerCategory;
    }

    @Override
    public GradleModelAssociation getModelAssociation() {
        return modelAssociation;
    }
}
