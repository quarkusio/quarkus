package io.quarkus.bootstrap.model.gradle;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * Gradle project component selected into the application graph.
 * <p>
 * The component records the roles and graph positions observed while resolving the paired application model. Empty sets
 * mean that the producer did not report the corresponding characteristic; consumers must not infer one from the project
 * path or output layout.
 */
public interface GradleProjectComponent extends Serializable {

    /** @return composite-build-safe identity of the Gradle project; never {@code null} */
    GradleProjectIdentity getProjectIdentity();

    /**
     * Returns the copied Gradle component identity used during resolution.
     * <p>
     * The value is opaque to consumers and is intended for correlation and diagnostics.
     *
     * @return non-{@code null} identity
     */
    String getSelectedComponentIdentity();

    /** @return non-{@code null} set of roles in which this project participated */
    Set<Role> getRoles();

    /** @return non-{@code null} set of classpaths on which this project was observed */
    Set<ClasspathAssociation> getClasspathAssociations();

    /** @return non-{@code null} set of relationships between this project and the target application graph */
    Set<GraphRelationship> getGraphRelationships();

    /** @return non-{@code null} list of source paths published for this project */
    List<? extends GradleSourceObservation> getSourceObservations();

    /** @return non-{@code null} list of logical class and resource outputs published for this project */
    List<? extends GradleLogicalOutput> getLogicalOutputs();

    /** Role of a Gradle project in the paired application model. */
    enum Role {
        /** Target application project. */
        APPLICATION,
        /** Workspace project selected as an application dependency. */
        WORKSPACE_DEPENDENCY,
        /** Project contributing a Quarkus extension runtime artifact. */
        EXTENSION_RUNTIME,
        /** Project contributing a Quarkus extension deployment artifact. */
        EXTENSION_DEPLOYMENT
    }

    /** Classpath on which the project was observed. */
    enum ClasspathAssociation {
        /** Runtime classpath. */
        RUNTIME,
        /** Quarkus augmentation/deployment classpath. */
        DEPLOYMENT,
        /** Compile-only classpath. */
        COMPILE_ONLY,
        /** Test-mode classpath association. */
        TEST
    }

    /** Relationship between the project and the target application graph. */
    enum GraphRelationship {
        /** Direct dependency relationship. */
        DIRECT,
        /** Transitive dependency relationship. */
        TRANSITIVE,
        /** Direct workspace-module dependency relationship recorded by the application model. */
        WORKSPACE_DIRECT
    }
}
