package io.quarkus.devtools.codestarts.core.strategy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.quarkus.devtools.codestarts.CodestartStructureException;
import io.quarkus.devtools.codestarts.core.reader.TargetFile;

public class DockerComposeCodestartFileStrategyHandler implements CodestartFileStrategyHandler {

    static final String NAME = "docker-compose-includes";

    final String INCLUDE_LINE_IDENTIFIER = "include:\n";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void process(Path targetDirectory, String relativePath, List<TargetFile> codestartFiles, Map<String, Object> data)
            throws IOException {
        List<String> includes = (List<String>) data.getOrDefault(NAME, new ArrayList<String>());

        for (TargetFile targetFile : codestartFiles) {
            String filename = targetFile.getSourceName();
            writeFile(targetDirectory.resolve(filename), targetFile.getContent());

            includes.add(filename);
        }
        data.put(NAME, includes);

        StringBuilder content = new StringBuilder(INCLUDE_LINE_IDENTIFIER);
        for (String include : includes) {
            content.append("  - ").append(include).append("\n");
        }

        final Path targetPath = targetDirectory.resolve("docker-compose.yml");
        writeFile(targetPath, content.toString());
    }

    @Override
    public void writeFile(final Path targetPath, final String content) throws IOException {
        if (Files.exists(targetPath) && !Files.readString(targetPath).startsWith(INCLUDE_LINE_IDENTIFIER)) {
            throw new CodestartStructureException(
                    "Target file already exists: " + targetPath.toString());
        }

        Files.writeString(targetPath, content);
    }

}
