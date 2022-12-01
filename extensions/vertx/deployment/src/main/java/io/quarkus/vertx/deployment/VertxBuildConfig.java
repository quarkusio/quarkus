package io.quarkus.vertx.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "vertx", phase = ConfigPhase.BUILD_TIME)
public class VertxBuildConfig {

    /**
     * If set to {@code true} then a customized current context factory (backed by a Vert.x duplicated local context) is used
     * for normal scopes in ArC.
     */
    @ConfigItem(generateDocumentation = false, defaultValue = "true")
    public boolean customizeArcContext;

}
