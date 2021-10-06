package io.quarkus.bootstrap.resolver.update;

import io.quarkus.maven.dependency.ResolvedDependency;
import java.util.List;

/**
 *
 * @author Alexey Loubyansky
 */
public interface UpdateDiscovery {

    List<String> listUpdates(ResolvedDependency artifact);

    String getNextVersion(ResolvedDependency artifact);

    String getLatestVersion(ResolvedDependency artifact);
}
