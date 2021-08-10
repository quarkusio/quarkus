package io.quarkus.openapi.generator.deployment.codegen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkus.bootstrap.prebuild.CodeGenException;
import io.quarkus.deployment.CodeGenContext;
import io.quarkus.deployment.CodeGenProvider;
import io.quarkus.openapi.generator.deployment.CodegenBuildTimeConfig;
import io.quarkus.openapi.generator.deployment.wrapper.OpenApiClientGeneratorWrapper;
import io.quarkus.utilities.OS;

/**
 * Code generation for OpenApi Client. Generates Java classes from OpenApi spec files located in src/main/openapi or
 * src/test/openapi
 * <p>
 * Wraps the <a href="https://openapi-generator.tech/docs/generators/java">OpenAPI Generator Client for Java</a>
 */
public abstract class OpenApiGeneratorCodeGenBase implements CodeGenProvider {

    static final String YAML = ".yaml";
    static final String YML = ".yml";
    static final String JSON = ".json";

    CodegenBuildTimeConfig codegenConfig;

    @Override
    public String inputDirectory() {
        return "openapi";
    }

    @Override
    public boolean trigger(CodeGenContext context) throws CodeGenException {
        final Path outDir = context.outDir();
        final Path openApiDir = context.inputDir();
        try {
            if (Files.isDirectory(openApiDir)) {
                try (Stream<Path> openApiFilesPaths = Files.walk(openApiDir)) {
                    final List<String> openApiFiles = openApiFilesPaths
                            .filter(Files::isRegularFile)
                            .map(Path::toString)
                            .filter(s -> s.endsWith(this.inputExtension()))
                            .map(this::escapeWhitespace)
                            .collect(Collectors.toList());
                    for (String openApiFile : openApiFiles) {
                        final OpenApiClientGeneratorWrapper generator = new OpenApiClientGeneratorWrapper(openApiFile,
                                outDir.toString());
                        generator.generate();
                    }
                    return true;
                }
            }
        } catch (IOException e) {
            throw new CodeGenException("Failed to generate java files from OpenApi files in " + openApiDir.toAbsolutePath(), e);
        }
        return false;
    }

    private String escapeWhitespace(String path) {
        if (OS.determineOS() == OS.LINUX) {
            return path.replace(" ", "\\ ");
        } else {
            return path;
        }
    }
}
