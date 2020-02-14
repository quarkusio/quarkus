package io.quarkus.bootstrap.resolver.update;

import io.quarkus.bootstrap.model.AppArtifact;
import java.util.List;

/**
 *
 * @author Alexey Loubyansky
 */
public interface UpdateDiscovery {

    List<String> listUpdates(AppArtifact artifact);

    String getNextVersion(AppArtifact artifact);

    String getLatestVersion(AppArtifact artifact);
}
