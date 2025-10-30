package io.quarkus.hibernate.spatial;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.hibernate-spatial")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface HibernateSpatialBuildTimeConfig {
    /**
     * Whether Hibernate Spatial is enabled <strong>during the build</strong>.
     * <p>
     * If Hibernate Spatial is disabled during the build, all processing related to Hibernate Spatial will be skipped.
     *
     * @asciidoclet
     */
    @WithDefault("true")
    boolean enabled();
}
