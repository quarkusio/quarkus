package io.quarkus.bootstrap.model.gradle;

import java.io.Serializable;

/**
 * Logical class or processed-resource output published by a Gradle producer.
 */
public interface GradleLogicalOutput extends Serializable {

    /**
     * Returns the sidecar-local identity used by producer-published
     * source-to-output associations.
     */
    String getIdentity();

    Kind getKind();

    Scope getScope();

    String getPath();

    /**
     * Returns the copied selected artifact or capability identity.
     */
    String getSelectedArtifactIdentity();

    String getProducerSemanticIdentity();

    String getSourceSet();

    String getJvmFeature();

    String getClassifier();

    Materialization getMaterialization();

    ProducerCategory getProducerCategory();

    GradleModelAssociation getModelAssociation();

    enum Kind {
        CLASSES,
        PROCESSED_RESOURCES
    }

    enum Scope {
        UNKNOWN,
        MAIN,
        TEST
    }

    enum Materialization {
        UNKNOWN,
        REQUIRED,
        OPTIONAL
    }

    enum ProducerCategory {
        UNKNOWN,
        STANDARD,
        GENERATED,
        OPAQUE
    }
}
