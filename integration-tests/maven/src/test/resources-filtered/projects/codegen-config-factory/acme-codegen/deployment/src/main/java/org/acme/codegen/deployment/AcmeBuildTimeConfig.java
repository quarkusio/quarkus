package org.acme.codegen.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.acme")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface AcmeBuildTimeConfig {

    /** codegen docs */
    public CodeGenConfig codegen();

    @ConfigGroup
    public interface CodeGenConfig {

        /** enabled docs */
        @WithDefault("true")
        public boolean enabled();

        /** stringOptional docs */
        public Optional<String> stringOptional();

    }
}