package io.quarkus.qute.debug.agent;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.lsp4j.debug.Source;

public class SourceTemplateRegistry {

    private final Map<String, Source> templateIdToSource = new HashMap<>();

    private final List<String> basePaths;
    private final List<String> fileExtensions;

    public SourceTemplateRegistry() {
        this(List.of("src/main/resources/templates/", /* Roq ... */"templates/", "content/"), //
                List.of(".qute", ".html", ".qute.html", ".yaml", ".qute.yaml", ".yml", ".qute.yml", ".txt", ".qute.txt",
                        ".md", ".qute.md"));
    }

    public SourceTemplateRegistry(List<String> basePaths, List<String> fileExtensions) {
        this.basePaths = basePaths;
        this.fileExtensions = fileExtensions;
    }

    public Source getSource(String templateId, Source previousSource) {
        var source = templateIdToSource.get(templateId);
        if (source == null) {
            for (var fileExtension : fileExtensions) {
                source = templateIdToSource.get(templateId + fileExtension);
                if (source != null) {
                    return source;
                }
            }

        }
        if (source == null) {
            if (previousSource != null) {
                String path = previousSource.getPath().replace("\\", "/");
                for (var basePath : basePaths) {
                    int index = path.indexOf(basePath);
                    if (index != -1) {
                        String sourcePath = getValidSourcePath(
                                path.substring(0, index + basePath.length()) + templateId);
                        if (sourcePath != null) {
                            source = new Source();
                            source.setPath(sourcePath);
                            templateIdToSource.put(templateId, source);
                        }
                    }
                }
            }
        }
        return source;
    }

    private String getValidSourcePath(String sourcePath) {
        if (Files.exists(Paths.get(sourcePath))) {
            return sourcePath;
        }
        for (var fileExtension : fileExtensions) {
            String sourcePathWithExt = sourcePath + fileExtension;
            if (Files.exists(Paths.get(sourcePathWithExt))) {
                return sourcePathWithExt;
            }
        }
        return null;
    }

    public void registerSource(Source source) {
        templateIdToSource.put(toTemplateId(source), source);
    }

    public String getTemplateId(Source source) {
        return toTemplateId(source);
    }

    private String toTemplateId(Source source) {
        String path = source.getPath().replace("\\", "/");
        for (var basePath : basePaths) {
            int index = path.indexOf(basePath);
            if (index != -1) {
                return path.substring(index + basePath.length());
            }
        }
        return null;
    }

    public List<String> getFileExtensions() {
        return fileExtensions;
    }
}
