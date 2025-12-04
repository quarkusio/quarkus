package io.quarkus.qute.debug.agent.source;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.debug.Source;

import io.quarkus.qute.Engine;
import io.quarkus.qute.JavaElementUriBuilder;
import io.quarkus.qute.debug.agent.breakpoints.BreakpointsRegistry;
import io.quarkus.qute.debug.client.JavaSourceLocationArguments;
import io.quarkus.qute.debug.client.JavaSourceLocationResponse;
import io.quarkus.qute.debug.client.JavaSourceResolver;

/**
 * Registry responsible for resolving and managing mappings between Qute
 * template IDs and their corresponding {@link RemoteSource} instances.
 * <p>
 * This class plays a key role in the Qute debugger by allowing the Debug
 * Adapter Protocol (DAP) to locate and serve the correct source file for a
 * given template, whether it originates from a local filesystem or a JAR.
 * </p>
 *
 * <p>
 * The registry maintains a cache of resolved template IDs to avoid redundant
 * lookups and supports flexible base paths and file extensions to locate
 * template files.
 * </p>
 */
public class SourceTemplateRegistry {

    private static final String JAR_SCHEME = "jar";

    private final Map<String /* templateId */, RemoteSource> templateIdToSource = new HashMap<>();

    private final Engine engine;
    private final List<String> basePaths;
    private final List<String> fileExtensions;

    private final BreakpointsRegistry breakpointsRegistry;
    private final SourceReferenceRegistry sourceReferenceRegistry;

    private final JavaSourceResolver javaSourceResolver;

    /**
     * Creates a registry with default base paths and file extensions.
     * <ul>
     * <li>Default base paths:
     * <ul>
     * <li>{@code src/main/resources/templates/}</li>
     * <li>{@code templates/}</li>
     * <li>{@code content/}</li>
     * </ul>
     * </li>
     * <li>Default file extensions:
     * <ul>
     * <li>.qute, .html, .qute.html</li>
     * <li>.yaml, .qute.yaml, .yml, .qute.yml</li>
     * <li>.txt, .qute.txt</li>
     * <li>.md, .qute.md</li>
     * </ul>
     * </li>
     * </ul>
     */
    public SourceTemplateRegistry(BreakpointsRegistry breakpointsRegistry,
            SourceReferenceRegistry sourceReferenceRegistry, JavaSourceResolver javaFileInfoProvider, Engine engine) {
        this(breakpointsRegistry, sourceReferenceRegistry, javaFileInfoProvider, engine,
                List.of("src/main/resources/templates/", "templates/", "content/"),
                List.of(".qute", ".html", ".qute.html", ".yaml", ".qute.yaml", ".yml", ".qute.yml", ".txt", ".qute.txt",
                        ".md", ".qute.md"));
    }

    /**
     * Creates a registry with custom base paths and file extensions.
     *
     * @param breakpointsRegistry registry managing active breakpoints
     * @param sourceReferenceRegistry registry responsible for DAP source references
     * @param engine the Qute template engine instance
     * @param basePaths list of possible base directories where
     *        templates might be located
     * @param fileExtensions list of supported file extensions for template
     *        files
     */
    public SourceTemplateRegistry(BreakpointsRegistry breakpointsRegistry,
            SourceReferenceRegistry sourceReferenceRegistry, JavaSourceResolver javaFileInfoProvider, Engine engine,
            List<String> basePaths, List<String> fileExtensions) {
        this.breakpointsRegistry = breakpointsRegistry;
        this.sourceReferenceRegistry = sourceReferenceRegistry;
        this.javaSourceResolver = javaFileInfoProvider;
        this.engine = engine;
        this.basePaths = basePaths;
        this.fileExtensions = fileExtensions;
    }

    /**
     * Attempts to resolve a {@link RemoteSource} for a given Qute template ID.
     * <p>
     * This method first checks the internal cache. If the source is not yet
     * registered, it attempts to resolve it from:
     * <ul>
     * <li>The {@link Engine} via {@link Engine#locate(String)}.</li>
     * <li>Previously known sources (from breakpoints or cached URIs).</li>
     * </ul>
     * </p>
     *
     * @param templateId the Qute template identifier
     * @param previousSource the previously known source, used to infer relative
     *        paths (optional)
     * @return the resolved {@link RemoteSource}, or {@code null} if none could be
     *         found
     */
    public RemoteSource getSource(String templateId, Source previousSource) {
        RemoteSource source = templateIdToSource.get(templateId);
        if (source != null) {
            return source; // Cached result
        }

        URI sourceUri = getSourceUriFromEngine(templateId, this.engine);
        if (sourceUri == null) {
            if (JavaElementUriBuilder.isJavaUri(templateId)) {
                // Template id defines a qute-java// uri
                // ex:
                // qute-java://org.acme.quarkus.sample.HelloResource$Hello@io.quarkus.qute.TemplateContents
                sourceUri = URI.create(templateId);
            } else {
                sourceUri = getGuessedSourceUri(templateId, previousSource);
            }
        }

        if (sourceUri != null) {
            if (JavaElementUriBuilder.isJavaUri(sourceUri)) {
                // ex:
                // qute-java://org.acme.quarkus.sample.HelloResource$Hello@io.quarkus.qute.TemplateContents
                JavaSourceLocationArguments args = parse(sourceUri.toString());
                try {
                    JavaSourceLocationResponse response = javaSourceResolver.resolveJavaSource(args).get(2000,
                            TimeUnit.MILLISECONDS);
                    if (response != null) {
                        URI javaSourceUri = URI.create(response.getJavaFileUri());
                        int startLine = response.getStartLine();
                        source = new JavaFileSource(javaSourceUri, templateId, startLine);
                        templateIdToSource.put(templateId, source);
                        return source;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (TimeoutException e) {
                    e.printStackTrace();
                }
                return null;
            }
            source = createSource(sourceUri, templateId);
            templateIdToSource.put(templateId, source);
            return source;
        }

        return null;
    }

    private static JavaSourceLocationArguments parse(String javaElementUri) {
        JavaSourceLocationArguments args = new JavaSourceLocationArguments();
        args.setJavaElementUri(javaElementUri);

        char[] chars = javaElementUri.toCharArray();
        int i = JavaElementUriBuilder.QUTE_JAVA_URI_PREFIX.length();
        int len = chars.length;

        StringBuilder type = new StringBuilder();
        StringBuilder method = new StringBuilder();
        StringBuilder annotation = new StringBuilder();

        // 0 = reading type
        // 1 = reading method (after '#')
        // 2 = reading annotation (after '@')
        int mode = 0;

        for (; i < len; i++) {
            char c = chars[i];

            switch (mode) {
                case 0: // reading type
                    if (c == '#') {
                        mode = 1;
                    } else if (c == '@') {
                        mode = 2;
                    } else {
                        type.append(c);
                    }
                    break;

                case 1: // reading method
                    if (c == '@') {
                        mode = 2;
                    } else {
                        method.append(c);
                    }
                    break;

                case 2: // reading annotation
                    annotation.append(c);
                    break;
            }
        }

        args.setTypeName(type.length() == 0 ? null : type.toString());
        args.setMethod(method.length() == 0 ? null : method.toString());
        args.setAnnotation(annotation.length() == 0 ? null : annotation.toString());

        return args;
    }

    /**
     * Creates a {@link RemoteSource} depending on the URI scheme.
     * <ul>
     * <li>If the URI scheme is "jar", creates a {@link JarSource} and registers a
     * source reference.</li>
     * <li>Otherwise, creates a {@link FileSource}.</li>
     * </ul>
     */
    private RemoteSource createSource(URI sourceUri, String templateId) {
        if (JAR_SCHEME.equals(sourceUri.getScheme())) {
            return new JarSource(sourceUri, templateId, sourceReferenceRegistry);
        }
        return new FileSource(sourceUri, templateId);
    }

    /**
     * Queries the {@link Engine} for the physical URI of a given template.
     *
     * @param templateId the Qute template ID
     * @param engine the engine instance
     * @return the URI if found, or {@code null} otherwise
     */
    private static URI getSourceUriFromEngine(String templateId, Engine engine) {
        var location = engine.locate(templateId);
        if (location.isPresent()) {
            var source = location.get().getSource();
            return source.orElse(null);
        }
        return null;
    }

    /**
     * Tries to infer the {@link URI} of a template based on its ID by checking
     * known source URIs, typical base paths, and possible file extensions.
     * <p>
     * This method is used as a fallback when the Qute engine cannot resolve a
     * template ID to a physical location.
     * </p>
     *
     * <ul>
     * <li>Searches in the registered breakpoint URIs and cached RemoteSources.</li>
     * <li>Supports multiple base paths (e.g. "templates/",
     * "META-INF/resources/").</li>
     * <li>Supports common file extensions (.html, .qute, .qute.html, etc.).</li>
     * </ul>
     *
     * @param templateId the Qute template identifier (e.g. "tags/ifError.html")
     * @param previousSource the previously resolved source, may be {@code null}
     * @param basePaths the possible root paths where templates may be located
     * @param extensions the possible file extensions to consider
     * @return the best-matching {@link URI}, or {@code null} if none matched
     */
    private URI getGuessedSourceUri(String templateId, Source previousSource) {

        Set<URI> knownUris = new HashSet<>(breakpointsRegistry.getSourceUris());
        knownUris.addAll(templateIdToSource.values().stream().map(RemoteSource::getUri).filter(Objects::nonNull)
                .collect(Collectors.toSet()));

        if (knownUris.isEmpty()) {
            return null;
        }

        String normalizedTemplateId = templateId.replace("\\", "/");

        // Generate candidate suffixes based on base paths + extensions
        List<String> candidates = new ArrayList<>();

        // Add plain templateId
        candidates.add(normalizedTemplateId);

        // Add all basePath + templateId variants
        for (String base : basePaths) {
            base = base.replace("\\", "/");
            if (!base.endsWith("/"))
                base += "/";
            candidates.add(base + normalizedTemplateId);
        }

        // Add extension variants (both direct and basePath-prefixed)
        List<String> allCandidates = new ArrayList<>();
        for (String c : candidates) {
            allCandidates.add(c);
            for (String ext : getFileExtensions()) {
                if (!c.endsWith(ext)) {
                    allCandidates.add(c + ext);
                }
            }
        }

        // Try to find a matching URI
        for (URI uri : knownUris) {
            String path = uri.getSchemeSpecificPart().replace("\\", "/");
            for (String candidate : allCandidates) {
                if (path.endsWith("/" + candidate) || path.endsWith(candidate)) {
                    return uri;
                }
            }
        }

        return null;
    }

    /**
     * Returns the list of supported file extensions.
     */
    public List<String> getFileExtensions() {
        return fileExtensions;
    }

    /**
     * Converts a {@link Source} object into a normalized {@link URI}. Supports
     * fallback for Windows paths and relative URIs.
     */
    public static URI toUri(Source source) {
        String path = source.getPath();
        if (path == null) {
            return null;
        }
        try {
            return normalize(Paths.get(path).toUri());
        } catch (Exception e) {
            try {
                return new URI("file", null, source.getPath(), null);
            } catch (URISyntaxException ignored) {
                return null;
            }
        }
    }

    /**
     * Normalizes file URIs (e.g. ensures consistent casing of Windows drive
     * letters).
     */
    private static URI normalize(URI uri) {
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            String path = uri.getPath();
            if (path.length() >= 3 && Character.isLetter(path.charAt(1)) && path.charAt(2) == ':') {
                path = "/" + Character.toUpperCase(path.charAt(1)) + path.substring(2);
                return URI.create("file://" + path);
            }
        }
        return uri;
    }
}
