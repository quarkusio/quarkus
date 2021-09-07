package io.quarkus.platform.catalog.predicate;

import static io.quarkus.platform.catalog.processor.ExtensionProcessor.getShortName;

import io.quarkus.platform.catalog.processor.ExtensionProcessor;
import io.quarkus.registry.catalog.Extension;
import java.util.Set;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A {@link Predicate} implementation that will test a keyword against an {@link Extension}
 */
public class ExtensionPredicate implements Predicate<Extension> {

    private final String q;

    public ExtensionPredicate(String keyword) {
        this.q = Objects.requireNonNull(keyword, "keyword must not be null").trim().toLowerCase();
    }

    public static Predicate<Extension> create(String keyword) {
        return new ExtensionPredicate(keyword);
    }

    public static boolean isPattern(String keyword) {
        for (char c : keyword.toCharArray()) {
            switch (c) {
                case '*':
                case '?':
                case '^': // escape character in cmd.exe
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
                    return true;
            }
        }
        return false;
    }

    @Override
    public boolean test(Extension extension) {
        final ExtensionProcessor extensionMetadata = ExtensionProcessor.of(extension);
        if (extensionMetadata.isUnlisted()) {
            return false;
        }

        String extensionName = Objects.toString(extension.getName(), "");
        String shortName = Objects.toString(extensionMetadata.getShortName(), "");

        // Try exact matches
        if (isExactMatch(extension)) {
            return true;
        }
        // Try short names
        if (matchesShortName(extension, q)) {
            return true;
        }
        // Partial matches on name, artifactId and short names
        if (extensionName.toLowerCase().contains(q)
                || extension.getArtifact().getArtifactId().toLowerCase().contains(q)
                || shortName.toLowerCase().contains(q)) {
            return true;
        }
        // find by keyword
        if (extensionMetadata.getExtendedKeywords().contains(q)) {
            return true;
        }
        // find by pattern
        Pattern pattern = toRegex(q);
        return pattern != null && (pattern.matcher(extensionName.toLowerCase()).matches()
                || pattern.matcher(extension.getArtifact().getArtifactId().toLowerCase()).matches()
                || pattern.matcher(shortName.toLowerCase()).matches()
                || matchLabels(pattern, extensionMetadata.getKeywords()));
    }

    public boolean isExactMatch(Extension extension) {
        String extensionName = Objects.toString(extension.getName(), "");
        // Try exact matches
        if (extensionName.equalsIgnoreCase(q) ||
                matchesArtifactId(extension.getArtifact().getArtifactId(), q)) {
            return true;
        }
        return false;
    }

    private static boolean matchesShortName(Extension extension, String q) {
        return q.equalsIgnoreCase(getShortName(extension));
    }

    private static boolean matchesArtifactId(String artifactId, String q) {
        return artifactId.equalsIgnoreCase(q) ||
                artifactId.equalsIgnoreCase("quarkus-" + q);
    }

    private static boolean matchLabels(Pattern pattern, Set<String> labels) {
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
                return Pattern.compile(wildcardToRegex, Pattern.CASE_INSENSITIVE);
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
        StringBuilder s = new StringBuilder(wildcard.length());
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
}
