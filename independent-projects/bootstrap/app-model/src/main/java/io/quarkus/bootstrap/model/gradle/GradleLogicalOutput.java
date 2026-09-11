package io.quarkus.bootstrap.model.gradle;

import java.io.Serializable;

/**
 * Logical class or processed-resource output published by a Gradle producer.
 * <p>
 * A project may expose multiple outputs of the same kind. Consumers must use {@link #getIdentity()} and the explicit
 * model association rather than assuming one output directory per project or reconstructing semantics from a path.
 */
public interface GradleLogicalOutput extends Serializable {

    /**
     * Returns the sidecar-local identity used by producer-published source-to-output associations.
     * <p>
     * The value is opaque to consumers and is unique only within its sidecar.
     *
     * @return non-{@code null} identity
     */
    String getIdentity();

    /** @return kind of files contained in this output; never {@code null} */
    Kind getKind();

    /** @return source scope reported by the producer, or {@link Scope#UNKNOWN}; never {@code null} */
    Scope getScope();

    /**
     * @return non-{@code null} path exactly as copied from Gradle; it is not an IDE-owned or necessarily portable path
     */
    String getPath();

    /**
     * Returns the copied selected artifact or capability identity.
     * <p>
     * The value is opaque and intended for correlation and diagnostics, not parsing.
     *
     * @return non-{@code null} identity
     */
    String getSelectedArtifactIdentity();

    /**
     * @return semantic identity published by the producer, or {@code null} when none was published
     */
    String getProducerSemanticIdentity();

    /** @return Gradle source-set name, or {@code null} when not known */
    String getSourceSet();

    /** @return Gradle JVM feature name, or {@code null} when not known */
    String getJvmFeature();

    /** @return producer-published artifact classifier, or {@code null} when not known */
    String getClassifier();

    /** @return materialization requirement reported by the producer; never {@code null} */
    Materialization getMaterialization();

    /** @return semantic category reported for the producer that owns this output; never {@code null} */
    ProducerCategory getProducerCategory();

    /** @return association with an exact application-model path occurrence; never {@code null} */
    GradleModelAssociation getModelAssociation();

    /** File family represented by a logical output. */
    enum Kind {
        /** JVM class-directory output. */
        CLASSES,
        /** Processed-resource-directory output. */
        PROCESSED_RESOURCES
    }

    /** Source scope to which the output belongs. */
    enum Scope {
        /** The producer did not expose a recognized scope. */
        UNKNOWN,
        /** Main/application source scope. */
        MAIN,
        /** Test source scope. */
        TEST
    }

    /** Materialization requirement reported by the producer. */
    enum Materialization {
        /** No requirement was reported. */
        UNKNOWN,
        /** The producer reports the output as required. */
        REQUIRED,
        /** The producer reports the output as optional. */
        OPTIONAL
    }

    /** Semantic category of the output producer. */
    enum ProducerCategory {
        /** The producer category was not recognized or reported. */
        UNKNOWN,
        /** Standard source-set producer. */
        STANDARD,
        /** Producer of generated sources or outputs. */
        GENERATED,
        /** Producer whose semantics are intentionally opaque to the consumer. */
        OPAQUE
    }
}
