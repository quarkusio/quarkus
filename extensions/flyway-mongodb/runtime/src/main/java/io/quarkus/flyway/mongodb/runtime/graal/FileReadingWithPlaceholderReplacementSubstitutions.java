package io.quarkus.flyway.mongodb.runtime.graal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.internal.parser.ParsingContext;
import org.flywaydb.core.internal.parser.PlaceholderReplacingReader;
import org.flywaydb.nc.FileReadingWithPlaceholderReplacement;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * In native image, migration scripts are embedded classpath resources rather than filesystem files.
 * {@code FileReadingWithPlaceholderReplacement.readFile} opens the script via {@link Files#newBufferedReader}
 * using the path returned by {@code ClassPathResource.getAbsolutePath()}, which is only the relative
 * classpath path and does not exist on the filesystem at runtime. This substitution tries the classloader
 * first and falls back to filesystem access for external-location scripts.
 */
@TargetClass(FileReadingWithPlaceholderReplacement.class)
public final class FileReadingWithPlaceholderReplacementSubstitutions {

    @Substitute
    public static String readFile(final Configuration configuration, final ParsingContext parsingContext,
            final String physicalLocation, final Charset encoding) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            var stream = classLoader.getResourceAsStream(physicalLocation);
            if (stream != null) {
                try (stream) {
                    PlaceholderReplacingReader reader = PlaceholderReplacingReader.create(configuration, parsingContext,
                            new InputStreamReader(stream, encoding));
                    try (BufferedReader bufferedReader = new BufferedReader(reader)) {
                        return String.join("\n", bufferedReader.lines().toList());
                    }
                }
            }
            PlaceholderReplacingReader reader = PlaceholderReplacingReader.create(configuration, parsingContext,
                    Files.newBufferedReader(Path.of(physicalLocation), encoding));
            try (BufferedReader bufferedReader = new BufferedReader(reader)) {
                return String.join("\n", bufferedReader.lines().toList());
            }
        } catch (IOException e) {
            throw new FlywayException(e);
        }
    }
}
