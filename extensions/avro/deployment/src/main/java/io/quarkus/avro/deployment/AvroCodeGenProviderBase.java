package io.quarkus.avro.deployment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.avro.generic.GenericData;
import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.prebuild.CodeGenException;
import io.quarkus.deployment.CodeGenContext;
import io.quarkus.deployment.CodeGenProvider;

public abstract class AvroCodeGenProviderBase implements CodeGenProvider {

    private static final Logger log = Logger.getLogger(AvroCodeGenProviderBase.class);

    /**
     * The directory (within the java classpath) that contains the velocity
     * templates to use for code generation.
     */
    static final String templateDirectory = "/org/apache/avro/compiler/specific/templates/java/classic/";

    @Override
    public String inputDirectory() {
        return "avro";
    }

    @Override
    public boolean trigger(CodeGenContext context) throws CodeGenException {
        init();
        boolean filesGenerated = false;
        AvroOptions options = new AvroOptions(context.config(), inputExtension());
        Path input = context.inputDir();
        Path outputDir = context.outDir();

        Set<Path> importedPaths = new HashSet<>();

        // compile the imports first
        for (String imprt : options.imports) {
            Path importPath = Paths.get(input.toAbsolutePath().toString(), imprt.trim()).toAbsolutePath();
            if (Files.isDirectory(importPath)) {
                for (Path file : gatherAllFiles(importPath)) {
                    compileSingleFile(file, outputDir, options);
                    importedPaths.add(file);
                    filesGenerated = true;
                }
            } else {
                compileSingleFile(importPath, outputDir, options);
                importedPaths.add(importPath);
                filesGenerated = true;
            }
        }

        // compile the rest of the files
        for (Path file : gatherAllFiles(input)) {
            if (!importedPaths.contains(file)) {
                compileSingleFile(file, outputDir, options);
                filesGenerated = true;
            }
        }

        return filesGenerated;
    }

    abstract void init();

    private Collection<Path> gatherAllFiles(Path importPath) throws CodeGenException {
        try {
            return Files.find(importPath, 20,
                    (path, ignored) -> Files.isRegularFile(path) && path.toString().endsWith("." + inputExtension()))
                    .map(Path::toAbsolutePath)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new CodeGenException("Failed to list matching files in " + importPath, e);
        }
    }

    abstract void compileSingleFile(Path importPath, Path outputDir, AvroOptions options) throws CodeGenException;

    public static class AvroOptions {

        public static final String[] EMPTY = new String[0];

        private final Config config;

        /**
         * A list of files or directories that should be compiled first thus making them
         * importable by subsequently compiled schemas. Note that imported files should
         * not reference each other.
         * <p>
         * All paths should be relative to the src/[main|test]/avro directory
         * <p>
         * Passed as a comma-separated list.
         */
        final String[] imports;

        /**
         * The Java type to use for Avro strings. May be one of CharSequence, String or
         * Utf8. CharSequence by default.
         */
        final GenericData.StringType stringType;

        /**
         * The createOptionalGetters parameter enables generating the getOptional...
         * methods that return an Optional of the requested type. This works ONLY on
         * Java 8+
         */
        final boolean createOptionalGetters;

        /**
         * Determines whether or not to use Java classes for decimal types, defaults to false
         */
        final boolean enableDecimalLogicalType;

        /**
         * Determines whether or not to create setters for the fields of the record. The
         * default is to create setters.
         */
        final boolean createSetters;

        /**
         * The gettersReturnOptional parameter enables generating get... methods that
         * return an Optional of the requested type. This will replace the This works
         * ONLY on Java 8+
         */
        final boolean gettersReturnOptional;

        /**
         * The optionalGettersForNullableFieldsOnly parameter works in conjunction with
         * gettersReturnOptional option. If it is set, Optional getters will be
         * generated only for fields that are nullable. If the field is mandatory,
         * regular getter will be generated. This works ONLY on Java 8+.
         */
        final boolean optionalGettersForNullableFieldsOnly;

        AvroOptions(Config config, String specificPropertyKey) {
            this.config = config;
            String imports = prop("avro.codegen." + specificPropertyKey + ".imports", "");
            this.imports = "".equals(imports) ? EMPTY : imports.split(",");

            stringType = GenericData.StringType.valueOf(prop("avro.codegen.stringType", "String"));
            createOptionalGetters = getBooleanProperty("avro.codegen.createOptionalGetters", false);
            enableDecimalLogicalType = getBooleanProperty("avro.codegen.enableDecimalLogicalType", false);
            createSetters = getBooleanProperty("avro.codegen.createSetters", true);
            gettersReturnOptional = getBooleanProperty("avro.codegen.gettersReturnOptional", false);
            optionalGettersForNullableFieldsOnly = getBooleanProperty("avro.codegen.optionalGettersForNullableFieldsOnly",
                    false);
        }

        private String prop(String propName, String defaultValue) {
            return config.getOptionalValue(propName, String.class).orElse(defaultValue);
        }

        private boolean getBooleanProperty(String propName, boolean defaultValue) {
            String value = prop(propName, String.valueOf(defaultValue)).toLowerCase(Locale.ROOT);
            if (Boolean.FALSE.toString().equals(value)) {
                return false;
            }
            if (Boolean.TRUE.toString().equals(value)) {
                return true;
            }
            return defaultValue;
        }
    }
}
