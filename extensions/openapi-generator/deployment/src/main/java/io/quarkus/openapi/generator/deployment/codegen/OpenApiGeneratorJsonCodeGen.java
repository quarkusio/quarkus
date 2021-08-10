package io.quarkus.openapi.generator.deployment.codegen;

public class OpenApiGeneratorJsonCodeGen extends OpenApiGeneratorCodeGenBase {

    @Override
    public String providerId() {
        return "open-api-json";
    }

    @Override
    public String inputExtension() {
        return JSON;
    }
}
