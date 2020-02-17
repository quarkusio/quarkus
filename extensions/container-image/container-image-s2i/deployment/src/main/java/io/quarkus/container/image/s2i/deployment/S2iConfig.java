package io.quarkus.container.image.s2i.deployment;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class S2iConfig {
}
