package io.quarkus.avro.deployment;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.avro.Protocol;
import org.apache.avro.compiler.idl.Idl;
import org.apache.avro.compiler.idl.ParseException;
import org.apache.avro.compiler.specific.SpecificCompiler;

import io.quarkus.bootstrap.prebuild.CodeGenException;
import io.quarkus.deployment.CodeGenProvider;

public class AvroIDLCodeGenProvider extends AvroCodeGenProviderBase implements CodeGenProvider {
    @Override
    public String providerId() {
        return "avdl";
    }

    @Override
    public String inputExtension() {
        return "avdl";
    }

    @Override
    void init() {

    }

    @Override
    void compileSingleFile(Path filePath, Path outputDir, AvroOptions options) throws CodeGenException {
        try (Idl parser = new Idl(filePath.toFile())) {
            Protocol idlProtocol = parser.CompilationUnit();
            String json = idlProtocol.toString(false);
            Protocol protocol = Protocol.parse(json);
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
            compiler.compileToDestination(filePath.toFile(), outputDir.toFile());
        } catch (IOException | ParseException e) {
            throw new CodeGenException("Failed to compile avro IDL file: " + filePath.toString() + " to Java", e);
        }
    }
}
