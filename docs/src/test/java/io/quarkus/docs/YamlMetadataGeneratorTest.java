package io.quarkus.docs;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import io.quarkus.docs.generation.YamlMetadataGenerator;
import io.quarkus.docs.generation.YamlMetadataGenerator.FileMessages;
import io.quarkus.docs.generation.YamlMetadataGenerator.Index;

public class YamlMetadataGeneratorTest {
    @Test
    @DisabledIfSystemProperty(named = "vale", matches = ".*", disabledReason = "Results included alongside Vale lint results")
    public void testAsciidocFiles() throws Exception {
        Path srcDir = Path.of("").resolve("src/main/asciidoc");
        Path targetDir = Path.of("").resolve("target");
        Path gitDir = ChangedFiles.getPath("git.dir", "../.git");

        YamlMetadataGenerator metadataGenerator = new YamlMetadataGenerator()
                .setSrcDir(srcDir)
                .setTargetDir(targetDir);

        Path baseDir = gitDir.getParent().toAbsolutePath().normalize();
        Path docsDir = baseDir.relativize(srcDir.toAbsolutePath());
        try (ChangedFiles git = new ChangedFiles(gitDir)) {
            Collection<String> files = git.modifiedFiles(docsDir, s -> s.replace(docsDir.toString() + "/", ""));
            if (files.isEmpty()) {
                System.out.println("\nNo pending changes to docs.\n");
                return; // EXIT EARLY
            } else {
                System.out.println("The following files will be inspected: ");
                files.forEach(System.out::println);
                metadataGenerator.setFileList(files);
            }
        }

        // Generate YAML: doc requirements
        Index index = metadataGenerator.generateIndex();
        Map<String, FileMessages> messages = index.messagesByFile();

        boolean hasErrors = false;
        StringBuilder sb = new StringBuilder("\n");
        for (String fileName : messages.keySet()) {
            FileMessages fm = messages.get(fileName);
            if (fm != null) {
                sb.append(fileName).append(": ").append("\n");
                hasErrors |= fm.listAll(sb);
            }
        }

        String result = sb.toString().trim();
        System.err.println(result);
        if (hasErrors) {
            throw new LintException("target/errorsByFile.yaml");
        } else {
            System.out.println("🥳 OK");
        }
    }
}
