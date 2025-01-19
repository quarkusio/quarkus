package io.quarkus.funqy.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.funqy")
public interface FunqyConfig {

    /**
     * The function to export. If there is more than one function
     * defined for this deployment, then you must set this variable.
     * If there is only a single function, you do not have to set this config item.
     *
     */
    Optional<String> export();
}
