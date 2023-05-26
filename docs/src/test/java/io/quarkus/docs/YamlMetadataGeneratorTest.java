package io.quarkus.docs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import io.quarkus.docs.generation.YamlMetadataGenerator;
import io.quarkus.docs.generation.YamlMetadataGenerator.FileMessages;
import io.quarkus.docs.generation.YamlMetadataGenerator.Index;

public class YamlMetadataGeneratorTest {
    String fileFilter = System.getProperty("docMetadataSources", "git");

    @Test
    @DisabledIfSystemProperty(named = "vale", matches = ".*", disabledReason = "Results included alongside Vale lint results")
    public void testAsciidocFiles() throws Exception {
        Path srcDir = Path.of("").resolve("src/main/asciidoc");
        Path targetDir = Path.of("").resolve("target");
        Path gitDir = ChangedFiles.getPath("git.dir", "../.git");

        YamlMetadataGenerator metadataGenerator = new YamlMetadataGenerator()
                .setSrcDir(srcDir)
                .setTargetDir(targetDir);

        if ("git".equals(fileFilter)) {
            Path baseDir = gitDir.getParent().toAbsolutePath().normalize();
            Path docsDir = baseDir.relativize(srcDir.toAbsolutePath());

            Collection<String> files = ChangedFiles.getChangedFiles(gitDir, docsDir);
            if (files.isEmpty()) {
                return; // EXIT EARLY
            }
            metadataGenerator.setFileList(files);
        } else {
            metadataGenerator.setFileFilterPattern(fileFilter);
        }

        // Generate YAML: doc requirements
        Index index = metadataGenerator.generateIndex();
        Map<String, FileMessages> messages = index.messagesByFile();

        boolean hasErrors = false;
        StringBuilder sb = new StringBuilder("\n");
        for (String fileName : messages.keySet()) {
            FileMessages fm = messages.get(fileName);
            if (fm != null) {
                sb.append("**").append(fileName).append(":**").append("\n\n");
                hasErrors |= fm.mdListAll(sb);
            }
        }

        String result = sb.toString().trim();
        System.err.println(result);

        Path metadataErrors = targetDir.resolve("metadataErrors.md");
        if (hasErrors) {
            result = "For a full list see target/errorsByType.xml and target/errorsByFile.xml\n\n" + result;

            Files.writeString(metadataErrors, result, StandardOpenOption.CREATE);
            throw new LintException("target/metadataErrors.md");
        } else {
            Files.deleteIfExists(metadataErrors);
            System.out.println("🥳 OK");
        }
    }
}
