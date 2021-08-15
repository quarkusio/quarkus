package io.quarkus.avro.deployment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.apache.avro.Schema;
import org.apache.avro.compiler.specific.SpecificCompiler;

import io.quarkus.bootstrap.prebuild.CodeGenException;
import io.quarkus.deployment.CodeGenProvider;

/**
 * Avro code generator for Avro Schema, based on the avro-maven-plugin
 *
 * @see AvroCodeGenProviderBase
 */
public class AvroSchemaCodeGenProvider extends AvroCodeGenProviderBase implements CodeGenProvider {

    Schema.Parser schemaParser;

    @Override
    public String providerId() {
        return "avsc";
    }

    @Override
    public String inputExtension() {
        return "avsc";
    }

    void init() {
        schemaParser = new Schema.Parser();
    }

    @Override
    void compileSingleFile(Path filePath,
            Path outputDirectory,
            AvroOptions options) throws CodeGenException {
        final Schema schema;

        File file = filePath.toFile();

        // This is necessary to maintain backward-compatibility. If there are
        // no imported files then isolate the schemas from each other, otherwise
        // allow them to share a single schema so reuse and sharing of schema
        // is possible.
        try {
            if (options.imports == AvroOptions.EMPTY) {
                schema = new Schema.Parser().parse(file);
            } else {
                schema = schemaParser.parse(file);
            }
        } catch (IOException e) {
            throw new CodeGenException("", e);
        }

        final SpecificCompiler compiler = new SpecificCompiler(schema);
        compiler.setTemplateDir(templateDirectory);
        compiler.setStringType(options.stringType);
        compiler.setFieldVisibility(SpecificCompiler.FieldVisibility.PRIVATE);
        compiler.setCreateOptionalGetters(options.createOptionalGetters);
        compiler.setGettersReturnOptional(options.gettersReturnOptional);
        compiler.setOptionalGettersForNullableFieldsOnly(options.optionalGettersForNullableFieldsOnly);
        compiler.setCreateSetters(options.createSetters);
        compiler.setEnableDecimalLogicalType(options.enableDecimalLogicalType);
        compiler.setOutputCharacterEncoding("UTF-8");
        try {
            compiler.compileToDestination(file, outputDirectory.toFile());
        } catch (IOException e) {
            throw new CodeGenException("Failed to copy compiled files to output directory " +
                    outputDirectory.toAbsolutePath(), e);
        }
    }
}
