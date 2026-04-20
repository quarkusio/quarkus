package io.quarkus.avro.deployment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.nio.file.Path;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.smallrye.config.SmallRyeConfigBuilder;

public class CompilationTest {

    private static final Config DEFAULT_CONFIG = new SmallRyeConfigBuilder()
            .withDefaultValue("avro.codegen.stringType", "String")
            .build();

    @TempDir
    Path outputDir;

    @Test
    public void testCanCompileIdlSchema() {
        compileFile(new AvroIDLCodeGenProvider(), "src/test/avro/schema.avdl");
    }

    @Test
    public void testCanCompileIdlProtocol() {
        compileFile(new AvroIDLCodeGenProvider(), "src/test/avro/protocol.avdl");
    }

    @Test
    public void testCanCompileAvscSchema() {
        compileFile(new AvroSchemaCodeGenProvider(), "src/test/avro/schema.avsc");
    }

    @Test
    public void testCanCompileAvprProtocol() {
        compileFile(new AvroProtocolCodeGenProvider(), "src/test/avro/protocol.avpr");
    }

    private void compileFile(AvroCodeGenProviderBase provider, String filePath) {
        AvroCodeGenProviderBase.AvroOptions options = provider.new AvroOptions(DEFAULT_CONFIG);
        Path sourceFile = Path.of(filePath).toAbsolutePath();
        assertDoesNotThrow(() -> provider.compileSingleFile(sourceFile, outputDir, options));
    }
}
