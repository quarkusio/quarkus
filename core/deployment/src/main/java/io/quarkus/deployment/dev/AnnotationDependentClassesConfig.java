package io.quarkus.deployment.dev;

import java.util.Optional;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
@ConfigMapping(prefix = "quarkus.dev")
public interface AnnotationDependentClassesConfig {

    /**
     * FQDNs of annotations that trigger automatic recompilation of annotated classes when their dependencies change
     * during dev mode. This is useful for annotation processors that generate code based on these classes (e.g. Mapstruct).
     */
    Optional<Set<String>> recompileAnnotations();
}
