package io.quarkus.openapi.generator.deployment;

import io.quarkus.openapi.generator.RuntimeConfig;
import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public class OpenApiGeneratorConfig {

    CodegenBuildTimeConfig codegenConfig;

    RuntimeConfig runtimeConfig;

    public OpenApiGeneratorConfig() {

    }

}
