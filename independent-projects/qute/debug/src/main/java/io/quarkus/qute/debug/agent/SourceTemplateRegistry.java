package io.quarkus.qute.debug.agent;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.lsp4j.debug.Source;

/**
 * Registry responsible for resolving and managing mappings between Qute
 * template IDs and their corresponding source files.
 * <p>
 * This class is used by the Qute debugger to locate template source files
 * during debugging sessions, supporting various base paths and file extensions.
 */
public class SourceTemplateRegistry {

    private final Map<String, Source> templateIdToSource = new HashMap<>();

    private final List<String> basePaths;
    private final List<String> fileExtensions;

    /**
     * Creates a registry with default base paths and file extensions.
     * <ul>
     * <li>Default base paths include:
     * <ul>
     * <li>{@code src/main/resources/templates/}</li>
     * <li>{@code templates/}</li>
     * <li>{@code content/}</li>
     * </ul>
     * </li>
     * <li>Default file extensions include:
     * <ul>
     * <li>.qute, .html, .qute.html</li>
     * <li>.yaml, .qute.yaml, .yml, .qute.yml</li>
     * <li>.txt, .qute.txt</li>
     * <li>.md, .qute.md</li>
     * </ul>
     * </li>
     * </ul>
     */
    public SourceTemplateRegistry() {
        this(List.of("src/main/resources/templates/", /* Roq ... */"templates/", "content/"), //
                List.of(".qute", ".html", ".qute.html", ".yaml", ".qute.yaml", ".yml", ".qute.yml", ".txt", ".qute.txt",
                        ".md", ".qute.md"));
    }

    /**
     * Creates a registry with custom base paths and file extensions.
     *
     * @param basePaths List of possible base directories where templates
     *        might be located
     * @param fileExtensions List of supported file extensions for template files
     */
    public SourceTemplateRegistry(List<String> basePaths, List<String> fileExtensions) {
        this.basePaths = basePaths;
        this.fileExtensions = fileExtensions;
    }

    /**
     * Attempts to resolve a {@link Source} for a given template ID.
     * <p>
     * If the source was previously registered, it will be returned directly.
     * Otherwise, this method will try to infer the source location using known
     * base paths and file extensions.
     *
     * @param templateId The Qute template identifier
     * @param previousSource The previously known source, used to infer relative
     *        paths
     * @return The resolved {@link Source} or {@code null} if none found
     */
    public Source getSource(String templateId, Source previousSource) {
        var source = templateIdToSource.get(templateId);
        if (source == null) {
            // Try with file extensions directly
            for (var fileExtension : fileExtensions) {
                source = templateIdToSource.get(templateId + fileExtension);
                if (source != null) {
                    return source;
                }
            }
        }

        if (source == null && previousSource != null) {
            // Try to infer source path relative to the previous source location
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
        return source;
    }

    /**
     * Validates if a source path exists on the filesystem. If the path without
     * extension does not exist, this method tries with each supported file
     * extension.
     *
     * @param sourcePath Base path to validate
     * @return The first valid path found, or {@code null} if none exist
     */
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

    /**
     * Registers a {@link Source} by associating it with its computed template ID.
     *
     * @param source The source to register
     */
    public void registerSource(Source source) {
        templateIdToSource.put(toTemplateId(source), source);
    }

    /**
     * Computes the template ID from a given source.
     *
     * @param source The source file
     * @return The template ID or {@code null} if it couldn't be determined
     */
    public String getTemplateId(Source source) {
        return toTemplateId(source);
    }

    /**
     * Converts a {@link Source} path to a template ID by stripping the base path.
     *
     * @param source The source
     * @return The template ID, or {@code null} if no base path matched
     */
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

    /**
     * Returns the list of supported file extensions.
     *
     * @return List of supported file extensions
     */
    public List<String> getFileExtensions() {
        return fileExtensions;
    }
}
