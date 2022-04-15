package io.quarkus.extest.runtime.config.named;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/* Causes a CCE: https://github.com/quarkusio/quarkus/issues/13966 */
@ConfigRoot(name = "same-simple-name-different-root", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class TestBuildAndRunTimeConfig {
}
