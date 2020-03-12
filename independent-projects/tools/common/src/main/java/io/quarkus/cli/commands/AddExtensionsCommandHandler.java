package io.quarkus.cli.commands;

import io.quarkus.cli.commands.file.BuildFile;
import io.quarkus.dependencies.Extension;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * This class is thread-safe. It extracts extensions to be added to the project from an instance of
 * {@link QuarkusCommandInvocation}.
 */
public class AddExtensionsCommandHandler implements QuarkusCommand {

    final static Printer PRINTER = new Printer();

    @Override
    public QuarkusCommandOutcome execute(QuarkusCommandInvocation invocation) throws QuarkusCommandException {

        final Set<String> extensions = invocation.getValue(AddExtensions.EXTENSIONS, Collections.emptySet());
        if (extensions.isEmpty()) {
            return QuarkusCommandOutcome.success().setValue(AddExtensions.OUTCOME_UPDATED, false);
        }

        boolean updated = false;
        boolean success = true;

        final List<Extension> registry = invocation.getPlatformDescriptor().getExtensions();

        final BuildFile buildFile = invocation.getBuildFile();
        try {
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
                            updated = buildFile.addDependency(invocation.getPlatformDescriptor(), extension) || updated;
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new QuarkusCommandException("Failed to add extensions", e);
        }

        if (updated) {
            try {
                buildFile.close();
            } catch (IOException e) {
                throw new QuarkusCommandException("Failed to update the project", e);
            }
        }

        return new QuarkusCommandOutcome(success).setValue(AddExtensions.OUTCOME_UPDATED, updated);
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

        extensions = extensions.stream().filter(e -> !e.isUnlisted()).collect(Collectors.toList());

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
