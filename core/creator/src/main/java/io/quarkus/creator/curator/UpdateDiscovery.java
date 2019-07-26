package io.quarkus.creator.curator;

import java.util.List;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.creator.AppCreatorException;

/**
 *
 * @author Alexey Loubyansky
 */
public interface UpdateDiscovery {

    List<String> listUpdates(AppArtifact artifact) throws AppCreatorException;

    String getNextVersion(AppArtifact artifact) throws AppCreatorException;

    String getLatestVersion(AppArtifact artifact) throws AppCreatorException;
}
