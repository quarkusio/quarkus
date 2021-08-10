package io.quarkus.openapi.generator.deployment.wrapper;

import org.openapitools.codegen.config.CodegenConfigurator;
import org.openapitools.codegen.languages.JavaClientCodegen;

public class QuarkusCodegenConfigurator extends CodegenConfigurator {

    public QuarkusCodegenConfigurator() {
        // immutable properties
        this.setGeneratorName("quarkus");
        this.setTemplatingEngineName("qute");
        this.setLibrary(JavaClientCodegen.MICROPROFILE);
    }
}
