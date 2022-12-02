package io.quarkus.docs.vale;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import io.quarkus.docs.ChangedFiles;
import io.quarkus.docs.LintException;
import io.quarkus.docs.generation.YamlMetadataGenerator;
import io.quarkus.docs.generation.YamlMetadataGenerator.Index;
import io.quarkus.docs.vale.ValeAsciidocLint.ChecksBySeverity;

public class LocalValeLintTest {
    @Test
    @EnabledIfSystemProperty(named = "vale", matches = ".*", disabledReason = "Requires a container runtime. Specifiy -Dvale to enable.")
    public void testAsciidocFiles() throws Exception {
        Path srcDir = Path.of("").resolve("src/main/asciidoc");
        Path targetDir = Path.of("").resolve("target");

        Path valeDir = ChangedFiles.getPath("vale.dir", ".vale");
        Path gitDir = ChangedFiles.getPath("git.dir", "../.git");

        YamlMetadataGenerator metadataGenerator = new YamlMetadataGenerator()
                .setSrcDir(srcDir)
                .setTargetDir(targetDir);

        ValeAsciidocLint linter = new ValeAsciidocLint()
                .setValeAlertLevel(System.getProperty("valeLevel"))
                .setValeImageName(System.getProperty("vale.image"))
                .setValeDir(valeDir)
                .setSrcDir(srcDir)
                .setTargetDir(targetDir);

        String valeFileFilter = System.getProperty("vale");
        if ("git".equals(valeFileFilter)) {
            Path baseDir = gitDir.getParent().toAbsolutePath().normalize();
            Path docsDir = baseDir.relativize(srcDir.toAbsolutePath());
            try (ChangedFiles git = new ChangedFiles(gitDir)) {
                Collection<String> files = git.modifiedFiles(docsDir, s -> s.replace(docsDir.toString() + "/", ""));
                if (files.isEmpty()) {
                    System.out.println("\nConfigured to analyze changed files: there are no pending changes.\n");
                    return;
                } else {
                    System.out.println("The following files will be inspected: ");
                    files.forEach(System.out::println);
                }
                metadataGenerator.setFileList(files);
                linter.setFileList(files);
            }
        } else {
            metadataGenerator.setFileFilterPattern(valeFileFilter);
            linter.setFileFilterPattern(valeFileFilter);
        }

        // Generate YAML: doc requirements
        Index index = metadataGenerator.generateIndex();
        Map<String, Collection<String>> metadataErrors = index.errorsByFile();

        // Find Vale errors
        Map<String, ChecksBySeverity> lintResults = linter.lintFiles();

        // Write vale.yaml
        linter.resultsToYaml(lintResults, metadataErrors);

        StringBuilder sb = new StringBuilder("\n");
        for (String fileName : lintResults.keySet()) {
            sb.append(fileName).append(": ").append("\n");

            if (!metadataErrors.isEmpty()) {
                sb.append("\n  metadata\n");
                Collection<String> mErrors = metadataErrors.getOrDefault(fileName, List.of());
                mErrors.forEach(e -> sb.append("    ").append(e).append("\n"));
            }

            ChecksBySeverity lErrors = lintResults.get(fileName);
            if (lErrors != null) {
                lErrors.checksBySeverity.entrySet().forEach(e -> {
                    sb.append("\n  ").append(e.getKey()).append("\n");
                    e.getValue().forEach(c -> sb.append("    ").append(c).append("\n"));
                });
            }
            sb.append("\n");
        }

        String result = sb.toString().trim();
        if (result.length() > 0) {
            System.err.println(result);
            throw new LintException("target/vale.yaml");
        } else {
            System.out.println("🥳 OK");
        }
    }
}
