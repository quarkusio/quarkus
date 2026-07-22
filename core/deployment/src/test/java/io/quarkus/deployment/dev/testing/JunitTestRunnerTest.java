package io.quarkus.deployment.dev.testing;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.jboss.jandex.DotName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JunitTestRunnerTest {

    @TempDir
    Path directory;

    @Test
    void indexesClassesFromEveryExistingTestOutputDirectory() throws IOException {
        Path firstOutput = Files.createDirectories(directory.resolve("java"));
        Path secondOutput = Files.createDirectories(directory.resolve("kotlin"));
        copyClass(FirstTestClass.class, firstOutput);
        copyClass(SecondTestClass.class, secondOutput);

        var index = JunitTestRunner.indexTestClasses(
                List.of(firstOutput, directory.resolve("missing"), secondOutput));

        assertThat(index.getClassByName(DotName.createSimple(FirstTestClass.class.getName()))).isNotNull();
        assertThat(index.getClassByName(DotName.createSimple(SecondTestClass.class.getName()))).isNotNull();
    }

    private static void copyClass(Class<?> type, Path outputDirectory) throws IOException {
        String classResource = type.getName().replace('.', '/') + ".class";
        Path target = outputDirectory.resolve(classResource);
        Files.createDirectories(target.getParent());
        try (InputStream source = Objects.requireNonNull(
                JunitTestRunnerTest.class.getClassLoader().getResourceAsStream(classResource))) {
            Files.copy(source, target);
        }
    }

    static final class FirstTestClass {
    }

    static final class SecondTestClass {
    }
}
