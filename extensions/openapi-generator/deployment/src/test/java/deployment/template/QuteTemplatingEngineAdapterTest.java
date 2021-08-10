package deployment.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;

import io.quarkus.openapi.generator.deployment.wrapper.QuarkusCodegenConfigurator;

public class QuteTemplatingEngineAdapterTest {

    @Test
    void checkTemplateGenerator() throws IOException {
        final String petstoreOpenApi = this.getClass().getResource("/openapi/petstore-openapi.json").getPath();
        final DefaultGenerator generator = new DefaultGenerator();
        final CodegenConfigurator configurator = new QuarkusCodegenConfigurator();
        final File apiFile = File.createTempFile("api", "java");
        apiFile.deleteOnExit();
        configurator.setInputSpec(petstoreOpenApi);
        generator.opts(configurator.toClientOptInput());

        final File writtenFile = generator.getTemplateProcessor().write(Collections.singletonMap("name", "Jack"), "hello.qute",
                apiFile);
        if (writtenFile != null) {
            writtenFile.deleteOnExit();
            assertEquals("Hello! My name is Jack", new String(Files.readAllBytes(writtenFile.toPath())));
        } else {
            fail("Template failed to write to the file");
        }
    }
}
