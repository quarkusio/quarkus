package io.quarkus.deployment.dev;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hard-coded mapping of Quarkus runtime artifacts to their required annotation processors.
 * <p>
 * This class maintains a central registry of which annotation processors should be
 * automatically configured when certain Quarkus extensions are used.
 */
public class AnnotationProcessorConfig {

    private static final Map<String, List<String>> PROCESSOR_MAPPINGS = new HashMap<>();

    static {
        // Map runtime artifact (groupId:artifactId) to annotation processor coordinates
        PROCESSOR_MAPPINGS.put("io.quarkus:quarkus-hibernate-panache",
                List.of("org.hibernate.orm:hibernate-jpamodelgen"));
        PROCESSOR_MAPPINGS.put("io.quarkus:quarkus-data-hibernate",
                List.of("io.quarkus:quarkus-data-processor"));
    }

    /**
     * Gets the annotation processors required for a given runtime artifact.
     *
     * @param groupId the runtime artifact's group ID
     * @param artifactId the runtime artifact's artifact ID
     * @return list of processor coordinates (groupId:artifactId), or empty list if none required
     */
    public static List<String> getProcessorsFor(String groupId, String artifactId) {
        String key = groupId + ":" + artifactId;
        return PROCESSOR_MAPPINGS.getOrDefault(key, Collections.emptyList());
    }

    /**
     * Gets all configured processor mappings.
     *
     * @return map of runtime artifact coordinates to processor coordinates
     */
    public static Map<String, List<String>> getAllMappings() {
        return Collections.unmodifiableMap(PROCESSOR_MAPPINGS);
    }

    private AnnotationProcessorConfig() {
        // Utility class
    }
}
