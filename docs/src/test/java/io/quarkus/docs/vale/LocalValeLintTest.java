package io.quarkus.docs.vale;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import io.quarkus.docs.ChangedFiles;
import io.quarkus.docs.LintException;
import io.quarkus.docs.generation.YamlMetadataGenerator;
import io.quarkus.docs.generation.YamlMetadataGenerator.FileMessages;
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

        Path configFile = Path.of(System.getProperty("vale.config", ".vale.ini"));

        YamlMetadataGenerator metadataGenerator = new YamlMetadataGenerator()
                .setSrcDir(srcDir)
                .setTargetDir(targetDir);

        ValeAsciidocLint linter = new ValeAsciidocLint()
                .setValeAlertLevel(System.getProperty("valeLevel"))
                .setValeImageName(System.getProperty("vale.image"))
                .setValeConfig(configFile)
                .setValeDir(valeDir)
                .setSrcDir(srcDir)
                .setTargetDir(targetDir);

        String valeFileFilter = System.getProperty("vale");
        if ("git".equals(valeFileFilter)) {
            Path baseDir = gitDir.getParent().toAbsolutePath().normalize();
            Path docsDir = baseDir.relativize(srcDir.toAbsolutePath());

            Collection<String> files = ChangedFiles.getChangedFiles(gitDir, docsDir);
            if (files.isEmpty()) {
                return; // EXIT EARLY
            }
            metadataGenerator.setFileList(files);
            linter.setFileList(files);
        } else {
            metadataGenerator.setFileFilterPattern(valeFileFilter);
            linter.setFileFilterPattern(valeFileFilter);
        }

        // Generate YAML: doc requirements
        Index index = metadataGenerator.generateIndex();
        Map<String, FileMessages> messages = index.messagesByFile();

        // Find Vale errors
        Map<String, ChecksBySeverity> lintResults = linter.lintFiles();

        // Write vale.yaml
        linter.resultsToYaml(lintResults, messages);

        boolean hasErrors = false;
        StringBuilder sb = new StringBuilder("\n");
        for (String fileName : lintResults.keySet()) {
            sb.append(fileName).append(": ").append("\n");

            FileMessages fm = messages.get(fileName);
            if (fm != null) {
                sb.append("\n  metadata\n");
                hasErrors |= fm.listAll(sb);
            }

            ChecksBySeverity lErrors = lintResults.get(fileName);
            if (lErrors != null) {
                hasErrors = true; // always fail in this purposeful case
                lErrors.checksBySeverity.entrySet().forEach(e -> {
                    sb.append("\n  ").append(e.getKey()).append("\n");
                    e.getValue().forEach(c -> sb.append("    ").append(c).append("\n"));
                });
            }
            sb.append("\n");
        }

        String result = sb.toString().trim();
        System.err.println(result);
        if (hasErrors) {
            throw new LintException("target/vale.yaml");
        } else {
            System.out.println("🥳 OK");
        }
    }
}
