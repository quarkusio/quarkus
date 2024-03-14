package io.quarkus.devtools.commands.handlers;

import static io.quarkus.devtools.messagewriter.MessageIcons.ERROR_ICON;
import static io.quarkus.devtools.utils.Patterns.isExpression;
import static io.quarkus.devtools.utils.Patterns.toRegex;
import static io.quarkus.platform.catalog.processor.ExtensionProcessor.getExtendedKeywords;
import static io.quarkus.platform.catalog.processor.ExtensionProcessor.getShortName;
import static io.quarkus.platform.catalog.processor.ExtensionProcessor.isUnlisted;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.SelectionResult;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.extensions.Extensions;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionCatalog;

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
                ArtifactCoords artifact = ArtifactCoords.fromString(query);
                final Extension ext = Extension.builder()
                        .setArtifact(artifact)
                        .setName(artifact.getArtifactId())
                        .build();
                builder.add(ext);
                continue;
            }

            SelectionResult result = null;
            if (countColons == 1) {
                final ArtifactKey key = ArtifactKey.fromString(query);
                for (Extension ext : extensionCatalog) {
                    if (ext.getArtifact().getKey().equals(key)) {
                        result = new SelectionResult(List.of(ext), true);
                        break;
                    }
                }
                if (result == null) {
                    result = new SelectionResult(List.of(), false);
                }
            } else {
                result = selectExtensions(query, extensionCatalog, false);
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
                candidates.forEach(extension -> sb.append(System.lineSeparator()).append("     - ")
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
     * Select extensions and return only one if exact match on the name or short name.
     */
    static SelectionResult selectExtensions(final String query, final Collection<Extension> allExtensions,
            boolean labelLookup) {
        return listExtensions(query, allExtensions, true, labelLookup);
    }

    /**
     * List extensions. Returns all matching extensions.
     */
    static SelectionResult listExtensions(final String query, final Collection<Extension> allExtensions,
            boolean labelLookup) {
        return listExtensions(query, allExtensions, false, labelLookup);
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
    private static SelectionResult listExtensions(final String query, final Collection<Extension> allExtensions,
            boolean returnOnExactMatch, boolean labelLookup) {
        String q = query.trim().toLowerCase();

        final Map<ArtifactKey, Extension> matches = new LinkedHashMap<>();

        if (!isExpression(q)) {
            // Try exact matches
            allExtensions.stream()
                    .filter(extension -> extension.getName().equalsIgnoreCase(q)
                            || matchesArtifactId(extension.getArtifact().getArtifactId(), q))
                    .forEach(e -> matches.putIfAbsent(e.getArtifact().getKey(), e));
            if (matches.size() == 1 && returnOnExactMatch) {
                return new SelectionResult(matches.values(), true);
            }

            final List<Extension> listedExtensions = getListedExtensions(allExtensions);

            // Try short names
            listedExtensions.stream().filter(extension -> matchesShortName(extension, q))
                    .forEach(e -> matches.putIfAbsent(e.getArtifact().getKey(), e));
            if (matches.size() == 1 && returnOnExactMatch) {
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
            if (matches.size() == 1 && returnOnExactMatch) {
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

    private static boolean matchLabels(Pattern pattern, Collection<String> labels) {
        boolean matches = false;
        // if any label match it's ok
        for (String label : labels) {
            matches = matches || pattern.matcher(label.toLowerCase()).matches();
        }
        return matches;
    }

    private static boolean matchesShortName(Extension extension, String q) {
        return q.equalsIgnoreCase(getShortName(extension));
    }

    private static boolean matchesArtifactId(String artifactId, String q) {
        return artifactId.equalsIgnoreCase(q) ||
                artifactId.equalsIgnoreCase("quarkus-" + q);
    }

}
