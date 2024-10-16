package io.quarkus.devtools.codestarts.core.strategy;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.devtools.codestarts.CodestartStructureException;
import io.quarkus.devtools.codestarts.core.reader.TargetFile;

/**
 *
 * @deprecated this was a quick-n-dirty way to allow extensions to provide content to the index.html,
 *             we don't need it anymore with the dynamic index.html. If we need something similar in the future let's find a
 *             more elegant way.
 */
@Deprecated
final class ContentMergeCodestartFileStrategyHandler implements CodestartFileStrategyHandler {

    static final String NAME = "content-merge";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void process(Path targetDirectory, String relativePath, List<TargetFile> codestartFiles, Map<String, Object> data)
            throws IOException {
        checkNotEmptyCodestartFiles(codestartFiles);
        final Path targetPath = targetDirectory.resolve(relativePath);
        Optional<TargetFile> template = Optional.empty();
        final StringBuilder mergedContent = new StringBuilder();
        for (TargetFile f : codestartFiles) {
            if (f.getContent().contains("{merged-content}")) {
                if (template.isPresent()) {
                    throw new CodestartStructureException("Found more than one file containing '{merged-content}': "
                            + f.getSourceName() + ", " + template.get().getSourceName());
                }
                template = Optional.of(f);
            } else {
                mergedContent.append(f.getContent());
            }
        }
        if (!template.isPresent() || mergedContent.length() == 0) {
            return;
        }
        createDirectories(targetPath);
        final String content = template.get().getContent();
        if (content.isBlank()) {
            return;
        }
        writeFile(targetPath, content.replace("{merged-content}", mergedContent.toString()));
    }
}
