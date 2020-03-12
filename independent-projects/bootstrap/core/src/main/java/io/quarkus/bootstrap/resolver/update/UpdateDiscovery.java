package io.quarkus.bootstrap.resolver.update;

import java.util.List;

import io.quarkus.bootstrap.model.AppArtifact;

/**
 *
 * @author Alexey Loubyansky
 */
public interface UpdateDiscovery {

    List<String> listUpdates(AppArtifact artifact);

    String getNextVersion(AppArtifact artifact);

    String getLatestVersion(AppArtifact artifact);
}
