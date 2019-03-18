package io.quarkus.camel.core.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "camel", phase = ConfigPhase.RUN_TIME)
public class CamelRuntimeConfig {
}
