package io.quarkus.annotation.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.tools.JavaFileObject;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.karuslabs.elementary.Results;
import com.karuslabs.elementary.junit.JavacExtension;
import com.karuslabs.elementary.junit.annotations.Classpath;
import com.karuslabs.elementary.junit.annotations.Processors;

@ExtendWith(JavacExtension.class)
@Processors({ ExtensionAnnotationProcessor.class })
@Disabled
class ExtensionAnnotationProcessorTest {

    @Test
    @Classpath("org.acme.examples.ClassWithBuildStep")
    void shouldProcessClassWithBuildStepWithoutErrors(Results results) throws IOException {
        assertNoErrors(results);
    }

    @Test
    @Classpath("org.acme.examples.ClassWithBuildStep")
    void shouldGenerateABscFile(Results results) throws IOException {
        assertNoErrors(results);
        List<JavaFileObject> sources = results.generatedSources;
        JavaFileObject bscFile = sources.stream()
                .filter(source -> source.getName()
                        .endsWith(".bsc"))
                .findAny()
                .orElse(null);
        assertNotNull(bscFile);

        String contents = removeLineBreaks(new String(bscFile
                .openInputStream()
                .readAllBytes(), StandardCharsets.UTF_8));
        assertEquals("org.acme.examples.ClassWithBuildStep", contents);
    }

    private String removeLineBreaks(String s) {
        return s.replace(System.getProperty("line.separator"), "")
                .replace("\n", "");
    }

    @Test
    @Classpath("org.acme.examples.ClassWithoutBuildStep")
    void shouldProcessEmptyClassWithoutErrors(Results results) {
        assertNoErrors(results);
    }

    private static void assertNoErrors(Results results) {
        assertEquals(0, results.find()
                .errors()
                .count(),
                "Errors were: " + results.find()
                        .errors()
                        .diagnostics());
    }
}
