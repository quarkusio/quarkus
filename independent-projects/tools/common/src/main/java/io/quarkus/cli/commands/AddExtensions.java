package io.quarkus.cli.commands;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.apache.maven.model.Dependency;

import io.quarkus.cli.commands.file.BuildFile;
import io.quarkus.cli.commands.file.MavenBuildFile;
import io.quarkus.cli.commands.writer.ProjectWriter;
import io.quarkus.dependencies.Extension;
import io.quarkus.generators.BuildTool;
import io.quarkus.maven.utilities.MojoUtils;
import io.quarkus.platform.tools.config.QuarkusPlatformConfig;

public class AddExtensions {

    private BuildFile buildFile;
    private final static Printer PRINTER = new Printer();

    public AddExtensions(final ProjectWriter writer) throws IOException {
        this(new MavenBuildFile(writer));
    }

    public AddExtensions(final ProjectWriter writer, final BuildTool buildTool) throws IOException {
        this.buildFile = buildTool.createBuildFile(writer);
    }

    public AddExtensions(final BuildFile buildFile) throws IOException {
        this.buildFile = buildFile;
    }

    /**
     * Selection algorithm.
     *
     * @param query the query
     * @param extensions the extension list
     * @param labelLookup whether or not the query must be tested against the labels of the extensions. Should
     *        be {@code false} by default.
     * @return the list of matching candidates and whether or not a match has been found.
     */
    static SelectionResult select(String query, List<Extension> extensions, boolean labelLookup) {
        String q = query.trim().toLowerCase();

        // Try exact matches
        Set<Extension> matchesNameOrArtifactId = extensions.stream()
                .filter(extension -> extension.getName().equalsIgnoreCase(q) || matchesArtifactId(extension.getArtifactId(), q))
                .collect(Collectors.toSet());
        if (matchesNameOrArtifactId.size() == 1) {
            return new SelectionResult(matchesNameOrArtifactId, true);
        }

        // Try short names
        Set<Extension> matchesShortName = extensions.stream().filter(extension -> matchesShortName(extension, q))
                .collect(Collectors.toSet());

        if (matchesShortName.size() == 1 && matchesNameOrArtifactId.isEmpty()) {
            return new SelectionResult(matchesShortName, true);
        }

        // Partial matches on name, artifactId and short names
        Set<Extension> partialMatches = extensions.stream().filter(extension -> extension.getName().toLowerCase().contains(q)
                || extension.getArtifactId().toLowerCase().contains(q)
                || extension.getShortName().toLowerCase().contains(q)).collect(Collectors.toSet());
        // Even if we have a single partial match, if the name, artifactId and short names are ambiguous, so not
        // consider it as a match.
        if (partialMatches.size() == 1 && matchesNameOrArtifactId.isEmpty() && matchesShortName.isEmpty()) {
            return new SelectionResult(partialMatches, true);
        }

        // find by labels
        List<Extension> matchesLabels;
        if (labelLookup) {
            matchesLabels = extensions.stream()
                    .filter(extension -> extension.labelsForMatching().contains(q)).collect(Collectors.toList());
        } else {
            matchesLabels = Collections.emptyList();
        }

        // find by pattern
        Set<Extension> matchesPatterns;
        Pattern pattern = toRegex(q);
        if (pattern != null) {
            matchesPatterns = extensions.stream()
                    .filter(extension -> pattern.matcher(extension.getName().toLowerCase()).matches()
                            || pattern.matcher(extension.getArtifactId().toLowerCase()).matches()
                            || pattern.matcher(extension.getShortName().toLowerCase()).matches()
                            || matchLabels(pattern, extension.getKeywords()))
                    .collect(Collectors.toSet());
            return new SelectionResult(matchesPatterns, true);
        } else {
            matchesPatterns = Collections.emptySet();
        }

        Set<Extension> candidates = new LinkedHashSet<>();
        candidates.addAll(matchesNameOrArtifactId);
        candidates.addAll(matchesShortName);
        candidates.addAll(partialMatches);
        candidates.addAll(matchesLabels);
        candidates.addAll(matchesPatterns);
        return new SelectionResult(candidates, false);
    }

    private static boolean matchLabels(Pattern pattern, List<String> labels) {
        boolean matches = false;
        // if any label match it's ok
        for(String label : labels) {
            matches = matches | pattern.matcher(label.toLowerCase()).matches();
        }
        return matches;
    }

    private static Pattern toRegex(final String str) {
        try {
            String wildcardToRegex = wildcardToRegex(str);
            if (wildcardToRegex != null && !wildcardToRegex.isEmpty()) {
                return Pattern.compile(wildcardToRegex);
            }
        } catch (PatternSyntaxException e) {
            //ignore it
        }
        return null;
    }

    public static String wildcardToRegex(String wildcard) {
        if (wildcard == null || wildcard.isEmpty()) {
            return null;
        }
        // don't try with file match char in pattern
        if (!(wildcard.contains("*") || wildcard.contains("?"))) {
            return null;
        }
        StringBuffer s = new StringBuffer(wildcard.length());
        s.append("^.*");
        for (int i = 0, is = wildcard.length(); i < is; i++) {
            char c = wildcard.charAt(i);
            switch (c) {
                case '*':
                    s.append(".*");
                    break;
                case '?':
                    s.append(".");
                    break;
                case '^': // escape character in cmd.exe
                    s.append("\\");
                    break;
                // escape special regexp-characters
                case '(':
                case ')':
                case '[':
                case ']':
                case '$':
                case '.':
                case '{':
                case '}':
                case '|':
                case '\\':
                    s.append("\\");
                    s.append(c);
                    break;
                default:
                    s.append(c);
                    break;
            }
        }
        s.append(".*$");
        return (s.toString());
    }

    private static boolean matchesShortName(Extension extension, String q) {
        return q.equalsIgnoreCase(extension.getShortName());
    }

    private static boolean matchesArtifactId(String artifactId, String q) {
        return artifactId.equalsIgnoreCase(q) ||
                artifactId.equalsIgnoreCase("quarkus-" + q);
    }

    public AddExtensionResult addExtensions(final Set<String> extensions) throws IOException {
        if (extensions == null || extensions.isEmpty()) {
            return new AddExtensionResult(false, true);
        }

        boolean updated = false;
        boolean success = true;
        List<Dependency> dependenciesFromBom = getDependenciesFromBom();

        List<Extension> registry = MojoUtils.loadExtensions();
        for (String query : extensions) {

            if (query.contains(":")) {
                // GAV case.
                updated = buildFile.addExtensionAsGAV(query) || updated;
            } else {
                SelectionResult result = select(query, registry, false);
                if (!result.matches()) {
                    StringBuilder sb = new StringBuilder();
                    // We have 3 cases, we can still have a single candidate, but the match is on label
                    // or we have several candidates, or none
                    Set<Extension> candidates = result.getExtensions();
                    if (candidates.isEmpty()) {
                        // No matches at all.
                        PRINTER.nok(" Cannot find a dependency matching '" + query + "', maybe a typo?");
                        success = false;
                    } else {
                        sb.append(Printer.NOK).append(" Multiple extensions matching '").append(query).append("'");
                        result.getExtensions()
                                .forEach(extension -> sb.append(System.lineSeparator()).append("     * ")
                                        .append(extension.managementKey()));
                        sb.append(System.lineSeparator())
                                .append("     Be more specific e.g using the exact name or the full GAV.");
                        PRINTER.print(sb.toString());
                        success = false;
                    }
                } else { // Matches.
                    for (Extension extension : result) {
                        // Don't set success to false even if the dependency is not added; as it's should be idempotent.
                        updated = buildFile.addDependency(dependenciesFromBom, extension) || updated;
                    }
                }
            }
        }

        if (updated) {
            buildFile.close();
        }

        return new AddExtensionResult(updated, success);
    }

    private List<Dependency> getDependenciesFromBom() {
        return QuarkusPlatformConfig.getGlobalDefault().getPlatformDescriptor().getManagedDependencies();
    }
}
