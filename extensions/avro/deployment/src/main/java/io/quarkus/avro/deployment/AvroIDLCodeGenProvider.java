package io.quarkus.avro.deployment;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.avro.Protocol;
import org.apache.avro.compiler.specific.SpecificCompiler;
import org.apache.avro.idl.IdlFile;
import org.apache.avro.idl.IdlReader;

import io.quarkus.bootstrap.prebuild.CodeGenException;
import io.quarkus.deployment.CodeGenProvider;

public class AvroIDLCodeGenProvider extends AvroCodeGenProviderBase implements CodeGenProvider {
    @Override
    public String providerId() {
        return "avdl";
    }

    @Override
    public String[] inputExtensions() {
        return new String[] { "avdl" };
    }

    @Override
    void init() {

    }

    @Override
    void compileSingleFile(Path filePath, Path outputDir, AvroOptions options) throws CodeGenException {
        try {
            IdlReader reader = new IdlReader();
            IdlFile idlFile = reader.parse(filePath);

            final SpecificCompiler compiler;
            Protocol protocol = idlFile.getProtocol();
            if (protocol != null) {
                compiler = new SpecificCompiler(protocol);
            } else {
                compiler = new SpecificCompiler(idlFile.getNamedSchemas().values());
            }

            compiler.setTemplateDir(templateDirectory);
            compiler.setStringType(options.stringType);
            compiler.setFieldVisibility(SpecificCompiler.FieldVisibility.PRIVATE);
            compiler.setCreateOptionalGetters(options.createOptionalGetters);
            compiler.setGettersReturnOptional(options.gettersReturnOptional);
            compiler.setOptionalGettersForNullableFieldsOnly(options.optionalGettersForNullableFieldsOnly);
            compiler.setCreateSetters(options.createSetters);
            compiler.setEnableDecimalLogicalType(options.enableDecimalLogicalType);

            compiler.setOutputCharacterEncoding("UTF-8");
            compiler.compileToDestination(filePath.toFile(), outputDir.toFile());
        } catch (IOException e) {
            throw new CodeGenException("Failed to compile avro IDL file: " + filePath.toString() + " to Java", e);
        }
    }
}
