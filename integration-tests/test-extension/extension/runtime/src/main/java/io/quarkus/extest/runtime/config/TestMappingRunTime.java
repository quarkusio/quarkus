package io.quarkus.extest.runtime.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.mapping.rt")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface TestMappingRunTime {
    /**
     * A String value.
     */
    String value();

    /**
     * A nested Group.
     */
    Group group();

    /** Record values from env test **/
    Optional<String> doNotRecord();

    /** Record values with named profile **/
    Optional<String> recordProfiled();

    /**
     * Record Default
     */
    @WithDefault("from-default")
    String recordDefault();

    /**
     * Secret
     */
    String recordSecret();

    /**
     * Override default with RunTimeConfigurationDefaultBuildItem
     */
    @WithDefault("from-default")
    String overrideBuildItem();

    /**
     * A expression value
     */
    @WithDefault("${quarkus.mapping.rt.expression.value}")
    Optional<String> expression();

    /**
     * Deprecated
     *
     * @deprecated deprecated.
     */
    @Deprecated
    String deprecated();

    interface Group {
        /**
         * A Group value.
         */
        String value();
    }
}
