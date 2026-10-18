package io.quarkus.bootstrap.model.gradle;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * Gradle project component selected into the application graph.
 */
public interface GradleProjectComponent extends Serializable {

    GradleProjectIdentity getProjectIdentity();

    /**
     * Returns the copied Gradle component identity used during resolution.
     */
    String getSelectedComponentIdentity();

    Set<Role> getRoles();

    Set<ClasspathAssociation> getClasspathAssociations();

    Set<GraphRelationship> getGraphRelationships();

    List<? extends GradleSourceObservation> getSourceObservations();

    List<? extends GradleLogicalOutput> getLogicalOutputs();

    enum Role {
        APPLICATION,
        WORKSPACE_DEPENDENCY,
        EXTENSION_RUNTIME,
        EXTENSION_DEPLOYMENT
    }

    enum ClasspathAssociation {
        RUNTIME,
        DEPLOYMENT,
        COMPILE_ONLY,
        TEST
    }

    enum GraphRelationship {
        DIRECT,
        TRANSITIVE,
        WORKSPACE_DIRECT
    }
}
