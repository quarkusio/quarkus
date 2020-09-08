package io.quarkus.devtools.commands.handlers;

import static io.quarkus.devtools.messagewriter.MessageIcons.ERROR_ICON;
import static io.quarkus.devtools.project.extensions.Extensions.toCoords;

import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.dependencies.Extension;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.SelectionResult;
import io.quarkus.devtools.project.extensions.Extensions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

final class QuarkusCommandHandlers {

    private QuarkusCommandHandlers() {
    }

    static List<AppArtifactCoords> computeCoordsFromQuery(final QuarkusCommandInvocation invocation,
            final Set<String> extensionsQuery) {
        final ArrayList<AppArtifactCoords> builder = new ArrayList<>();
        for (String query : extensionsQuery) {
            final int countColons = StringUtils.countMatches(query, ":");
            if (countColons == 1) {
                builder.add(toCoords(AppArtifactKey.fromString(query), null));
            } else if (countColons > 1) {
                builder.add(AppArtifactCoords.fromString(query));
            } else {
                Collection<Extension> extensions = invocation.getPlatformDescriptor().getExtensions();
                SelectionResult result = select(query, extensions, false);
                if (result.matches()) {
                    final Set<AppArtifactCoords> withStrippedVersion = result.getExtensions().stream().map(Extensions::toCoords)
                            .map(Extensions::stripVersion).collect(Collectors.toSet());
                    // We strip the version because those extensions are managed
                    builder.addAll(withStrippedVersion);
                } else {
                    StringBuilder sb = new StringBuilder();
                    // We have 3 cases, we can still have a single candidate, but the match is on label
                    // or we have several candidates, or none
                    Set<Extension> candidates = result.getExtensions();
                    if (candidates.isEmpty()) {
                        // No matches at all.
                        invocation.log().error("Cannot find a dependency matching '" + query + "', maybe a typo?");
                        return null;
                    } else {
                        sb.append(ERROR_ICON + " Multiple extensions matching '").append(query).append("'");
                        result.getExtensions()
                                .forEach(extension -> sb.append(System.lineSeparator()).append("     * ")
                                        .append(extension.managementKey()));
                        sb.append(System.lineSeparator())
                                .append("     Be more specific e.g using the exact name or the full GAV.");
                        invocation.log().info(sb.toString());
                        return null;
                    }
                }
            }
        }
        return builder;
    }

    /**
     * Selection algorithm.
     *
     * @param query the query
     * @param allPlatformExtensions the list of all platform extensions
     * @param labelLookup whether or not the query must be tested against the labels of the extensions. Should
     *        be {@code false} by default.
     * @return the list of matching candidates and whether or not a match has been found.
     */
    static SelectionResult select(final String query, final Collection<Extension> allPlatformExtensions,
            final boolean labelLookup) {
        String q = query.trim().toLowerCase();

        // Try exact matches
        Set<Extension> matchesNameOrArtifactId = allPlatformExtensions.stream()
                .filter(extension -> extension.getName().equalsIgnoreCase(q) || matchesArtifactId(extension.getArtifactId(), q))
                .collect(Collectors.toSet());
        if (matchesNameOrArtifactId.size() == 1) {
            return new SelectionResult(matchesNameOrArtifactId, true);
        }

        final List<Extension> listedPlatformExtensions = allPlatformExtensions.stream()
                .filter(e -> !e.isUnlisted()).collect(Collectors.toList());

        // Try short names
        Set<Extension> matchesShortName = listedPlatformExtensions.stream().filter(extension -> matchesShortName(extension, q))
                .collect(Collectors.toSet());

        if (matchesShortName.size() == 1 && matchesNameOrArtifactId.isEmpty()) {
            return new SelectionResult(matchesShortName, true);
        }

        // Partial matches on name, artifactId and short names
        Set<Extension> partialMatches = listedPlatformExtensions.stream()
                .filter(extension -> extension.getName().toLowerCase().contains(q)
                        || extension.getArtifactId().toLowerCase().contains(q)
                        || extension.getShortName().toLowerCase().contains(q))
                .collect(Collectors.toSet());
        // Even if we have a single partial match, if the name, artifactId and short names are ambiguous, so not
        // consider it as a match.
        if (partialMatches.size() == 1 && matchesNameOrArtifactId.isEmpty() && matchesShortName.isEmpty()) {
            return new SelectionResult(partialMatches, true);
        }

        // find by labels
        List<Extension> matchesLabels;
        if (labelLookup) {
            matchesLabels = listedPlatformExtensions.stream()
                    .filter(extension -> extension.labelsForMatching().contains(q)).collect(Collectors.toList());
        } else {
            matchesLabels = Collections.emptyList();
        }

        // find by pattern
        Set<Extension> matchesPatterns;
        Pattern pattern = toRegex(q);
        if (pattern != null) {
            matchesPatterns = listedPlatformExtensions.stream()
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
        for (String label : labels) {
            matches = matches || pattern.matcher(label.toLowerCase()).matches();
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

    private static String wildcardToRegex(String wildcard) {
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

}
