package io.quarkus.vertx.deployment;

import io.quarkus.runtime.annotations.ConfigDocIgnore;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.vertx")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface VertxBuildConfig {

    /**
     * If set to {@code true} then a customized current context factory (backed by a Vert.x duplicated local context) is
     * used for normal scopes in ArC.
     */
    @ConfigDocIgnore
    @WithDefault("true")
    public boolean customizeArcContext();

}
