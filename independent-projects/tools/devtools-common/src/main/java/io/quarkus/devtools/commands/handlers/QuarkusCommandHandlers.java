package io.quarkus.devtools.commands.handlers;

import static io.quarkus.devtools.messagewriter.MessageIcons.ERROR_ICON;
import static io.quarkus.platform.catalog.processor.ExtensionProcessor.getExtendedKeywords;
import static io.quarkus.platform.catalog.processor.ExtensionProcessor.getShortName;
import static io.quarkus.platform.catalog.processor.ExtensionProcessor.isUnlisted;

import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.SelectionResult;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.extensions.Extensions;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.maven.ArtifactKey;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.json.JsonExtension;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

final class QuarkusCommandHandlers {

    private QuarkusCommandHandlers() {
    }

    static List<Extension> computeExtensionsFromQuery(ExtensionCatalog catalog,
            final Set<String> extensionsQuery, MessageWriter log) {
        final ArrayList<Extension> builder = new ArrayList<>();
        final Collection<Extension> extensionCatalog = catalog.getExtensions();
        for (String query : extensionsQuery) {
            final int countColons = StringUtils.countMatches(query, ":");
            if (countColons > 1) {
                final JsonExtension ext = new JsonExtension();
                ext.setArtifact(ArtifactCoords.fromString(query));
                ext.setName(ext.getArtifact().getArtifactId());
                builder.add(ext);
                continue;
            }

            SelectionResult result = null;
            if (countColons == 1) {
                final ArtifactKey key = ArtifactKey.fromString(query);
                for (Extension ext : extensionCatalog) {
                    if (ext.getArtifact().getKey().equals(key)) {
                        result = new SelectionResult(Collections.singletonList(ext), true);
                        break;
                    }
                }
                if (result == null) {
                    result = new SelectionResult(Collections.emptyList(), false);
                }
            } else {
                result = select(query, extensionCatalog, false);
            }
            if (result.matches()) {
                builder.addAll(result.getExtensions());
            } else {
                StringBuilder sb = new StringBuilder();
                // We have 3 cases, we can still have a single candidate, but the match is on label
                // or we have several candidates, or none
                final Collection<Extension> candidates = result.getExtensions();
                if (candidates.isEmpty()) {
                    // No matches at all.
                    log.error("Cannot find a dependency matching '" + query + "', maybe a typo?");
                    return null;
                }
                sb.append(ERROR_ICON + " Multiple extensions matching '").append(query).append("'");
                candidates.forEach(extension -> sb.append(System.lineSeparator()).append("     * ")
                        .append(extension.managementKey()));
                sb.append(System.lineSeparator())
                        .append("     try using the exact name or the full GAV (group id, artifact id, and version).");
                log.info(sb.toString());
                return null;
            }

        }
        return builder;
    }

    static List<ArtifactCoords> computeCoordsFromQuery(final QuarkusCommandInvocation invocation,
            final Set<String> extensionsQuery) {
        final List<Extension> extensions = computeExtensionsFromQuery(invocation.getExtensionsCatalog(), extensionsQuery,
                invocation.log());
        return extensions == null ? null
                : extensions.stream().map(e -> Extensions.stripVersion(e.getArtifact())).collect(Collectors.toList());
    }

    /**
     * Selection algorithm.
     *
     * @param query the query
     * @param allExtensions the list of all platform extensions
     * @param labelLookup whether or not the query must be tested against the labels of the extensions. Should
     *        be {@code false} by default.
     * @return the list of matching candidates and whether or not a match has been found.
     */
    static SelectionResult select(final String query, final Collection<Extension> allExtensions,
            final boolean labelLookup) {
        String q = query.trim().toLowerCase();

        final Map<ArtifactKey, Extension> matches = new LinkedHashMap<>();

        if (!isExpression(q)) {
            // Try exact matches
            allExtensions.stream()
                    .filter(extension -> extension.getName().equalsIgnoreCase(q)
                            || matchesArtifactId(extension.getArtifact().getArtifactId(), q))
                    .forEach(e -> matches.putIfAbsent(e.getArtifact().getKey(), e));
            if (matches.size() == 1) {
                return new SelectionResult(matches.values(), true);
            }

            final List<Extension> listedExtensions = getListedExtensions(allExtensions);

            // Try short names
            listedExtensions.stream().filter(extension -> matchesShortName(extension, q))
                    .forEach(e -> matches.putIfAbsent(e.getArtifact().getKey(), e));
            if (matches.size() == 1) {
                return new SelectionResult(matches.values(), true);
            }

            // Partial matches on name, artifactId and short names
            listedExtensions.stream()
                    .filter(extension -> extension.getName().toLowerCase().contains(q)
                            || extension.getArtifact().getArtifactId().toLowerCase().contains(q)
                            || getShortName(extension).toLowerCase().contains(q))
                    .forEach(e -> matches.putIfAbsent(e.getArtifact().getKey(), e));
            // Even if we have a single partial match, if the name, artifactId and short names are ambiguous, so not
            // consider it as a match.
            if (matches.size() == 1) {
                return new SelectionResult(matches.values(), true);
            }

            // find by labels
            if (labelLookup) {
                listedExtensions.stream()
                        .filter(extension -> getExtendedKeywords(extension).contains(q))
                        .forEach(e -> matches.put(e.getArtifact().getKey(), e));
            }
            return new SelectionResult(matches.values(), false);
        }
        final List<Extension> listedExtensions = getListedExtensions(allExtensions);
        // find by pattern
        Pattern pattern = toRegex(q);
        if (pattern != null) {
            listedExtensions.stream()
                    .filter(extension -> pattern.matcher(extension.getName().toLowerCase()).matches()
                            || pattern.matcher(extension.getArtifact().getArtifactId().toLowerCase()).matches()
                            || pattern.matcher(getShortName(extension).toLowerCase()).matches()
                            || matchLabels(pattern, getExtendedKeywords(extension)))
                    .forEach(e -> matches.putIfAbsent(e.getArtifact().getKey(), e));
        }
        return new SelectionResult(matches.values(), !matches.isEmpty());
    }

    private static List<Extension> getListedExtensions(final Collection<Extension> allExtensions) {
        return allExtensions.stream()
                .filter(e -> !isUnlisted(e)).collect(Collectors.toList());
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
        // don't try with file match char in pattern
        if (!isExpression(wildcard)) {
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

    private static boolean isExpression(String s) {
        return s == null || s.isEmpty() ? false : s.contains("*") || s.contains("?");
    }

    private static boolean matchesShortName(Extension extension, String q) {
        return q.equalsIgnoreCase(getShortName(extension));
    }

    private static boolean matchesArtifactId(String artifactId, String q) {
        return artifactId.equalsIgnoreCase(q) ||
                artifactId.equalsIgnoreCase("quarkus-" + q);
    }

}
