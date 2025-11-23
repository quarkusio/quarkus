package io.quarkus.annotation.processor.documentation.config.merger;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import io.quarkus.annotation.processor.Outputs;
import io.quarkus.annotation.processor.documentation.config.model.JavadocElements;
import io.quarkus.annotation.processor.documentation.config.model.JavadocElements.JavadocElement;
import io.quarkus.annotation.processor.documentation.config.util.JacksonMappers;

public final class JavadocMerger {

    private JavadocMerger() {
    }

    public static JavadocRepository mergeJavadocElements(List<Path> buildOutputDirectories) {
        return mergeJavadocElements(new BuildOutputDirectoriesJavadocElementsReader(buildOutputDirectories));
    }

    public static JavadocRepository mergeJavadocElementsFromClassPathElements(List<Path> classPathElements) {
        return mergeJavadocElements(new ClassPathElementsJavadocElementsReader(classPathElements));
    }

    private static JavadocRepository mergeJavadocElements(JavadocElementsReader javadocElementsReader) {
        Map<String, JavadocElement> javadocElementsMap = new TreeMap<>();

        javadocElementsReader.consume(javadocElements -> {
            javadocElementsMap.putAll(javadocElements.elements());
        });

        return new JavadocRepository(javadocElementsMap);
    }

    private static class BuildOutputDirectoriesJavadocElementsReader implements JavadocElementsReader {

        private final List<Path> buildOutputDirectories;

        private BuildOutputDirectoriesJavadocElementsReader(List<Path> buildOutputDirectories) {
            this.buildOutputDirectories = buildOutputDirectories;
        }

        @Override
        public void consume(Consumer<JavadocElements> consumer) {
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

                    consumer.accept(javadocElements);
                } catch (IOException e) {
                    throw new UncheckedIOException("Unable to parse: " + javadocPath, e);
                }
            }
        }
    }

    private static class ClassPathElementsJavadocElementsReader implements JavadocElementsReader {

        private final List<Path> classPathElements;

        private ClassPathElementsJavadocElementsReader(List<Path> classPathElements) {
            this.classPathElements = classPathElements;
        }

        @Override
        public void consume(Consumer<JavadocElements> consumer) {
            for (Path classPathElement : classPathElements) {
                Path javadocPath = classPathElement.resolve(Outputs.META_INF_QUARKUS_CONFIG_JAVADOC_JSON);
                if (!Files.isReadable(javadocPath)) {
                    continue;
                }

                try (InputStream javadocIs = Files.newInputStream(javadocPath)) {
                    JavadocElements javadocElements = JacksonMappers.jsonObjectReader().readValue(javadocIs,
                            JavadocElements.class);

                    if (javadocElements.elements() == null || javadocElements.elements().isEmpty()) {
                        continue;
                    }

                    consumer.accept(javadocElements);
                } catch (IOException e) {
                    throw new UncheckedIOException("Unable to parse: " + javadocPath, e);
                }
            }
        }
    }

    private interface JavadocElementsReader {

        void consume(Consumer<JavadocElements> consumer);
    }
}
