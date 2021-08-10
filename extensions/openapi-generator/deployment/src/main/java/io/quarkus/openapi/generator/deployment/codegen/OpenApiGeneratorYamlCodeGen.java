package io.quarkus.openapi.generator.deployment.codegen;

public class OpenApiGeneratorYamlCodeGen extends OpenApiGeneratorCodeGenBase {

    @Override
    public String providerId() {
        return "open-api-yaml";
    }

    @Override
    public String inputExtension() {
        return YAML;
    }
}
