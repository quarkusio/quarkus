package io.quarkus.annotation.processor.documentation.config.merger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.annotation.processor.Outputs;
import io.quarkus.annotation.processor.documentation.config.model.JavadocElements;
import io.quarkus.annotation.processor.documentation.config.model.JavadocElements.JavadocElement;
import io.quarkus.annotation.processor.documentation.config.util.JacksonMappers;

public final class JavadocMerger {

    private JavadocMerger() {
    }

    public static JavadocRepository mergeJavadocElements(List<Path> buildOutputDirectories) {
        Map<String, JavadocElement> javadocElementsMap = new HashMap<>();

        for (Path buildOutputDirectory : buildOutputDirectories) {
            Path javadocPath = buildOutputDirectory.resolve(Outputs.QUARKUS_CONFIG_DOC_JAVADOC);
            if (!Files.isReadable(javadocPath)) {
                continue;
            }

            try (InputStream javadocIs = Files.newInputStream(javadocPath)) {
                JavadocElements javadocElements = JacksonMappers.yamlObjectReader().readValue(javadocIs,
                        JavadocElements.class);

                if (javadocElements.elements() == null || javadocElements.elements().isEmpty()) {
                    continue;
                }

                javadocElementsMap.putAll(javadocElements.elements());
            } catch (IOException e) {
                throw new IllegalStateException("Unable to parse: " + javadocPath, e);
            }
        }

        return new JavadocRepository(javadocElementsMap);
    }
}
