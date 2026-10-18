package io.quarkus.flyway.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.flywaydb.core.api.ResourceProvider;
import org.flywaydb.core.api.resource.LoadableResource;
import org.flywaydb.core.internal.util.StringUtils;
import org.jboss.logging.Logger;

/**
 * This class is very similar to {@link org.flywaydb.core.internal.scanner.Scanner}
 * TODO: refactor upstream to move common methods to utility class
 */
public class QuarkusFlywayResourceProvider implements ResourceProvider {

    private static final Logger log = Logger.getLogger(FlywayRecorder.class);

    private final Collection<LoadableResource> resources;

    public QuarkusFlywayResourceProvider(Collection<LoadableResource> resources) {
        this.resources = resources;
    }

    @Override
    public LoadableResource getResource(String name) {
        for (LoadableResource resource : resources) {
            String fileName = resource.getRelativePath();
            if (fileName.equals(name)) {
                return resource;
            }
        }
        return null;
    }

    /**
     * Returns all known resources starting with the specified prefix and ending with any of the specified suffixes.
     *
     * @param prefix The prefix of the resource names to match.
     * @param suffixes The suffixes of the resource names to match.
     * @return The resources that were found.
     */
    public Collection<LoadableResource> getResources(String prefix, String... suffixes) {
        List<LoadableResource> result = new ArrayList<>();
        for (LoadableResource resource : resources) {
            String fileName = resource.getFilename();
            if (StringUtils.startsAndEndsWith(fileName, prefix, suffixes)) {
                result.add(resource);
            } else {
                log.debug("Filtering out resource: " + resource.getAbsolutePath() + " (filename: " + fileName + ")");
            }
        }
        return result;
    }
}
