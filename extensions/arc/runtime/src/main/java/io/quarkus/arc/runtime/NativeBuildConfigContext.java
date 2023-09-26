package io.quarkus.arc.runtime;

import java.util.Set;

import io.quarkus.arc.config.NativeBuildTime;

/**
 * @see NativeBuildTime
 * @see NativeBuildConfigCheckInterceptor
 */
public interface NativeBuildConfigContext {

    /**
     *
     * @return the injected BUILD_AND_RUN_TIME_FIXED properties
     */
    Set<String> getBuildAndRunTimeFixed();
}
