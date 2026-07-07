package io.quarkus.bootstrap.model.gradle.impl;

import java.util.Objects;

import io.quarkus.bootstrap.model.gradle.GradleLogicalOutput;
import io.quarkus.bootstrap.model.gradle.GradleModelAssociation;

/**
 * Default serializable value used by Gradle producers to transport a {@link GradleLogicalOutput}.
 * <p>
 * Required fields are validated at construction time. Optional semantic fields remain {@code null} when the producer
 * cannot publish them; this implementation does not derive them from the output path.
 */
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

    /**
     * Creates a logical output value.
     *
     * @param identity opaque sidecar-local output identity; must not be {@code null}
     * @param kind kind of files in the output; must not be {@code null}
     * @param scope source scope reported by the producer; must not be {@code null}
     * @param path path copied from Gradle; must not be {@code null}
     * @param selectedArtifactIdentity opaque selected artifact or capability identity; must not be {@code null}
     * @param producerSemanticIdentity producer semantic identity, or {@code null} when unavailable
     * @param sourceSet Gradle source-set name, or {@code null} when unavailable
     * @param jvmFeature Gradle JVM feature name, or {@code null} when unavailable
     * @param classifier artifact classifier, or {@code null} when unavailable
     * @param materialization materialization requirement reported by the producer; must not be {@code null}
     * @param producerCategory semantic category of the producer; must not be {@code null}
     * @param modelAssociation exact application-model association or an explicit unknown association; must not be
     *        {@code null}
     * @throws NullPointerException if a required argument is {@code null}
     */
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
