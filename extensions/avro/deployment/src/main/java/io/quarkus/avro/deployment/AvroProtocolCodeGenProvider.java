package io.quarkus.avro.deployment;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.avro.Protocol;
import org.apache.avro.compiler.specific.SpecificCompiler;

import io.quarkus.bootstrap.prebuild.CodeGenException;
import io.quarkus.deployment.CodeGenProvider;

/**
 * Avro code generator for Avro Protocol, based on the avro-maven-plugin
 *
 * @see AvroCodeGenProviderBase
 */
public class AvroProtocolCodeGenProvider extends AvroCodeGenProviderBase implements CodeGenProvider {

    @Override
    public String providerId() {
        return "avpr";
    }

    @Override
    public String inputExtension() {
        return "avpr";
    }

    @Override
    void init() {
    }

    void compileSingleFile(Path filePath,
            Path outputDirectory,
            AvroOptions options) throws CodeGenException {
        try {
            final Protocol protocol = Protocol.parse(filePath.toFile());
            final SpecificCompiler compiler = new SpecificCompiler(protocol);
            compiler.setTemplateDir(templateDirectory);
            compiler.setStringType(options.stringType);
            compiler.setFieldVisibility(SpecificCompiler.FieldVisibility.PRIVATE);
            compiler.setCreateOptionalGetters(options.createOptionalGetters);
            compiler.setGettersReturnOptional(options.gettersReturnOptional);
            compiler.setOptionalGettersForNullableFieldsOnly(options.optionalGettersForNullableFieldsOnly);
            compiler.setCreateSetters(options.createSetters);
            compiler.setEnableDecimalLogicalType(options.enableDecimalLogicalType);

            compiler.setOutputCharacterEncoding("UTF-8");
            compiler.compileToDestination(filePath.toFile(), outputDirectory.toFile());
        } catch (IOException e) {
            throw new CodeGenException("Failed to compile avro protocole file: " + filePath.toString() + " to Java", e);
        }
    }
}
