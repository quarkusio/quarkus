package io.quarkus.openapi.generator.deployment.codegen;

public class OpenApiGeneratorYmlCodeGen extends OpenApiGeneratorCodeGenBase {

    @Override
    public String providerId() {
        return "open-api-yml";
    }

    @Override
    public String inputExtension() {
        return YML;
    }
}
