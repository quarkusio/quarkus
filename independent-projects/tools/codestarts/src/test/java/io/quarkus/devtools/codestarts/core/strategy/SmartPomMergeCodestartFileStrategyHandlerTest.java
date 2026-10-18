package io.quarkus.devtools.codestarts.core.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.devtools.codestarts.core.reader.TargetFile;

class SmartPomMergeCodestartFileStrategyHandlerTest {

    @TempDir
    Path tempDir;

    @Test
    void mergeShouldPreserveTargetPackaging() throws Exception {
        String basePom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.acme</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1.0.0-SNAPSHOT</version>
                    <packaging>quarkus</packaging>
                </project>
                """;

        String extensionPom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <properties>
                        <property-from-extension>value</property-from-extension>
                    </properties>
                </project>
                """;

        List<TargetFile> codestartFiles = List.of(
                new TargetFile("pom.xml", basePom),
                new TargetFile("pom.xml", extensionPom));

        Map<String, Object> data = Map.of("input", Map.of("base-codestart", Map.of("buildtool", "maven")));

        SmartPomMergeCodestartFileStrategyHandler handler = new SmartPomMergeCodestartFileStrategyHandler();
        handler.process(tempDir, "pom.xml", codestartFiles, data);

        String result = Files.readString(tempDir.resolve("pom.xml"));
        assertThat(result).contains("<packaging>quarkus</packaging>");
    }
}
