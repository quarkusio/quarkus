package io.quarkus.openapi.generator.deployment.wrapper;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import java.io.File;
import java.util.List;

import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.GlobalSettings;

/**
 * Wrapper for the OpenAPIGen tool.
 * This is the same as calling the Maven plugin or the CLI.
 * We are wrapping into a class to generate code that meet our requirements.
 *
 * @see <a href="https://openapi-generator.tech/docs/generators/java">OpenAPI Generator Client for Java</a>
 */
public class OpenApiClientGeneratorWrapper {

    private static final String MODEL_PACKAGE = "model";
    private static final String VERBOSE = "verbose";
    private static final String ONCE_LOGGER = "org.openapitools.codegen.utils.oncelogger.enabled";
    private static final String MVN_MAIN = "src/main/java";

    private final QuarkusCodegenConfigurator configurator;
    private final DefaultGenerator generator;
    private final String outputDir;

    public OpenApiClientGeneratorWrapper(final String specFilePath, final String outputDir) {
        this.outputDir = outputDir;
        // TODO: expose these properties as quarkus.generate-code.openapi.client.*
        // do not generate docs nor tests
        GlobalSettings.setProperty(CodegenConstants.API_DOCS, FALSE.toString());
        GlobalSettings.setProperty(CodegenConstants.API_TESTS, FALSE.toString());
        GlobalSettings.setProperty(CodegenConstants.MODEL_TESTS, FALSE.toString());
        GlobalSettings.setProperty(CodegenConstants.MODEL_DOCS, FALSE.toString());
        // generates every Api and Supporting files
        GlobalSettings.setProperty(CodegenConstants.APIS, "");
        GlobalSettings.setProperty(CodegenConstants.MODELS, "");
        GlobalSettings.setProperty(CodegenConstants.SUPPORTING_FILES, "");
        // logging
        GlobalSettings.setProperty(VERBOSE, FALSE.toString());
        GlobalSettings.setProperty(ONCE_LOGGER, TRUE.toString());

        this.configurator = new QuarkusCodegenConfigurator();
        this.configurator.setInputSpec(specFilePath);
        this.configurator.setOutputDir(outputDir);
        this.generator = new DefaultGenerator();
    }

    public OpenApiClientGeneratorWrapper withPackage(final String pkg) {
        this.configurator.setApiPackage(pkg);
        this.configurator.setInvokerPackage(pkg);
        this.configurator.setModelPackage(pkg + "." + MODEL_PACKAGE);
        return this;
    }

    public List<File> generate() {
        return generator.opts(configurator.toClientOptInput()).generate();
    }
}
