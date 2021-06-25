package io.quarkus.openapi.generator.deployment.wrapper;

import org.openapitools.codegen.languages.JavaClientCodegen;

public class QuarkusJavaClientCodegen extends JavaClientCodegen {

    public QuarkusJavaClientCodegen() {
        // TODO: immutable properties
        this.setDateLibrary(JavaClientCodegen.JAVA8_MODE);
        this.setTemplateDir("templates");
        // we are only interested in the main generated classes
        this.projectFolder = "";
        this.projectTestFolder = "";
        this.sourceFolder = "";
        this.testFolder = "";
    }

    @Override
    public String getName() {
        return "quarkus";
    }

    @Override
    public void processOpts() {
        super.processOpts();
        supportingFiles.clear();
        apiTemplateFiles.clear();
        apiTemplateFiles.put("api.qute", ".java");
        modelTemplateFiles.clear();
        modelTemplateFiles.put("model.qute", ".java");
    }
}
