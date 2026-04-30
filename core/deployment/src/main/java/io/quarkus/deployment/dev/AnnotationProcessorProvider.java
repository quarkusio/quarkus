package io.quarkus.deployment.dev;

import java.util.List;

/**
 * Service provider interface for extensions to declare required annotation processors.
 * <p>
 * Extensions implement this interface and register via META-INF/services to automatically
 * configure annotation processors in Maven and Gradle builds.
 * <p>
 * The providing extension's Maven coordinate is automatically determined from the JAR's
 * META-INF/maven/{groupId}/{artifactId}/pom.properties.
 */
public interface AnnotationProcessorProvider {

    /**
     * Returns the list of annotation processors required by this extension.
     * Each processor is specified as a Maven coordinate (groupId:artifactId).
     * Version can be omitted if managed by Quarkus BOM.
     *
     * @return list of annotation processor coordinates (e.g., "org.hibernate.orm:hibernate-processor")
     */
    List<String> getAnnotationProcessors();
}
