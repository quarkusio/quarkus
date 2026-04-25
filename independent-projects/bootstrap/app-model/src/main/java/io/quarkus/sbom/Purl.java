package io.quarkus.sbom;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A lightweight, immutable Package URL (PURL) representation following the
 * <a href="https://github.com/package-url/purl-spec">purl-spec</a>.
 * <p>
 * Format: {@code pkg:<type>/<namespace>/<name>@<version>?<qualifiers>#<subpath>}
 */
public final class Purl {

    // The /*  after pkg: tolerates pkg:// (spec: parsers MUST accept and strip extra slashes)
    private static final Pattern PURL_PATTERN = Pattern.compile(
            "^pkg:/*([^/]+)/([^@?#]+)(?:@([^?#]*))?(?:\\?([^#]*))?(?:#(.*))?$");

    public static final String TYPE_MAVEN = "maven";
    public static final String TYPE_NPM = "npm";
    public static final String TYPE_GENERIC = "generic";

    /**
     * Creates a Maven PURL with {@code type=jar} qualifier.
     *
     * @param groupId Maven groupId (used as namespace)
     * @param artifactId Maven artifactId (used as name)
     * @param version artifact version
     * @return a Maven PURL
     */
    public static Purl maven(String groupId, String artifactId, String version) {
        return maven(groupId, artifactId, version, "jar", null);
    }

    /**
     * Creates a Maven PURL with explicit type and classifier qualifiers.
     *
     * @param groupId Maven groupId (used as namespace)
     * @param artifactId Maven artifactId (used as name)
     * @param version artifact version
     * @param artifactType Maven artifact type (e.g., "jar", "war")
     * @param classifier Maven classifier, or null
     * @return a Maven PURL
     */
    public static Purl maven(String groupId, String artifactId, String version,
            String artifactType, String classifier) {
        Objects.requireNonNull(groupId, "groupId is required for Maven PURLs");
        Objects.requireNonNull(artifactId, "artifactId is required for Maven PURLs");
        String type = artifactType != null ? artifactType : "jar";
        Map<String, String> qualifiers;
        if (classifier != null && !classifier.isEmpty()) {
            var map = new TreeMap<String, String>();
            map.put("classifier", classifier);
            map.put("type", type);
            qualifiers = Collections.unmodifiableMap(map);
        } else {
            qualifiers = Map.of("type", type);
        }
        return new Purl(TYPE_MAVEN, groupId, artifactId, version, qualifiers, null);
    }

    /**
     * Creates an npm PURL.
     *
     * @param namespace the npm scope (e.g., "@babel"), or null for unscoped packages
     * @param name the package name
     * @param version the package version
     * @return an npm PURL
     */
    public static Purl npm(String namespace, String name, String version) {
        return new Purl(TYPE_NPM, namespace, name, version, Collections.emptyMap(), null);
    }

    /**
     * Creates a generic PURL with no namespace or qualifiers.
     *
     * @param name the component name
     * @param version the component version
     * @return a generic PURL
     */
    public static Purl generic(String name, String version) {
        return new Purl(TYPE_GENERIC, null, name, version, Collections.emptyMap(), null);
    }

    /**
     * Creates a PURL with the specified type.
     *
     * @param type the package ecosystem type
     * @param namespace the namespace, or null
     * @param name the package name
     * @param version the version, or null
     * @return a PURL
     */
    public static Purl of(String type, String namespace, String name, String version) {
        return new Purl(type, namespace, name, version, Collections.emptyMap(), null);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Parses a canonical PURL string.
     *
     * @param purlString a PURL string starting with "pkg:"
     * @return the parsed Purl
     * @throws IllegalArgumentException if the string is not a valid PURL
     */
    public static Purl parse(String purlString) {
        Objects.requireNonNull(purlString, "purlString is null");
        Matcher m = PURL_PATTERN.matcher(purlString);
        if (!m.matches()) {
            throw new IllegalArgumentException("Invalid PURL: " + purlString);
        }

        String type = m.group(1);
        String namespaceName = m.group(2);
        String versionRaw = m.group(3);
        String qualifiersRaw = m.group(4);
        String subpathRaw = m.group(5);

        String namespace = null;
        String name;
        int lastSlashIdx = namespaceName.lastIndexOf('/');
        if (lastSlashIdx < 0) {
            name = percentDecode(namespaceName);
        } else {
            name = percentDecode(namespaceName.substring(lastSlashIdx + 1));
            namespace = decodePath(namespaceName.substring(0, lastSlashIdx));
        }

        String version = versionRaw != null && !versionRaw.isEmpty() ? percentDecode(versionRaw) : null;

        Map<String, String> qualifiers = Map.of();
        if (qualifiersRaw != null) {
            TreeMap<String, String> parsed = null;
            String singleKey = null;
            String singleValue = null;
            int pairStart = 0;
            int len = qualifiersRaw.length();
            while (pairStart <= len) {
                int ampIdx = qualifiersRaw.indexOf('&', pairStart);
                int pairEnd = ampIdx < 0 ? len : ampIdx;
                int eqIdx = qualifiersRaw.indexOf('=', pairStart);
                if (eqIdx < 0 || eqIdx > pairEnd) {
                    throw new IllegalArgumentException(
                            "Invalid PURL qualifier '" + qualifiersRaw.substring(pairStart, pairEnd)
                                    + "': missing '=' in " + purlString);
                }
                if (eqIdx == pairStart) {
                    throw new IllegalArgumentException(
                            "Invalid PURL qualifier '" + qualifiersRaw.substring(pairStart, pairEnd)
                                    + "': empty key in " + purlString);
                }
                String key = qualifiersRaw.substring(pairStart, eqIdx).toLowerCase();
                String value = percentDecode(qualifiersRaw.substring(eqIdx + 1, pairEnd));
                if (parsed != null) {
                    parsed.put(key, value);
                } else if (singleKey != null) {
                    parsed = new TreeMap<>();
                    parsed.put(singleKey, singleValue);
                    parsed.put(key, value);
                } else {
                    singleKey = key;
                    singleValue = value;
                }
                pairStart = pairEnd + 1;
            }
            if (parsed != null) {
                qualifiers = Collections.unmodifiableMap(parsed);
            } else {
                qualifiers = Map.of(singleKey, singleValue);
            }
        }

        String subpath = subpathRaw != null ? decodePath(subpathRaw) : null;
        if (subpath != null && subpath.isEmpty()) {
            subpath = null;
        }

        return new Purl(type, namespace, name, version, qualifiers, subpath);
    }

    private final String type;
    private final String namespace;
    private final String name;
    private final String version;
    private final Map<String, String> qualifiers;
    private final String subpath;
    private String canonical;

    private Purl(String type, String namespace, String name, String version,
            Map<String, String> qualifiers, String subpath) {
        Objects.requireNonNull(type, "type is required");
        if (type.isEmpty()) {
            throw new IllegalArgumentException("type must not be empty");
        }
        validateType(type);
        this.type = type.toLowerCase(Locale.ROOT);
        this.namespace = namespace == null || namespace.isEmpty() ? null : namespace;
        Objects.requireNonNull(name, "name is required");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("name must not be empty");
        }
        if (TYPE_MAVEN.equals(this.type) && this.namespace == null) {
            throw new IllegalArgumentException("Maven PURLs require a namespace (groupId)");
        }
        this.name = TYPE_NPM.equals(this.type) ? name.toLowerCase(Locale.ROOT) : name;
        this.version = version;
        this.qualifiers = qualifiers == null || qualifiers.isEmpty()
                ? Map.of()
                : qualifiers;
        this.subpath = normalizeSubpath(subpath);
    }

    public String getType() {
        return type;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public Map<String, String> getQualifiers() {
        return qualifiers;
    }

    public String getSubpath() {
        return subpath;
    }

    /**
     * Returns the canonical PURL string representation.
     */
    @Override
    public String toString() {
        if (canonical == null) {
            canonical = canonicalize();
        }
        return canonical;
    }

    private String canonicalize() {
        int estimatedLength = 5 + type.length()
                + (namespace != null ? namespace.length() + 1 : 0)
                + name.length()
                + (version != null ? version.length() + 1 : 0);
        StringBuilder sb = new StringBuilder(estimatedLength);
        sb.append("pkg:").append(type).append('/');
        if (namespace != null) {
            sb.append(encodePath(namespace)).append('/');
        }
        sb.append(percentEncode(name));
        if (version != null) {
            sb.append('@').append(percentEncode(version));
        }
        if (!qualifiers.isEmpty()) {
            sb.append('?');
            boolean first = true;
            for (Map.Entry<String, String> entry : qualifiers.entrySet()) {
                if (!first) {
                    sb.append('&');
                }
                sb.append(entry.getKey())
                        .append('=')
                        .append(percentEncodeQualifierValue(entry.getValue()));
                first = false;
            }
        }
        if (subpath != null) {
            sb.append('#').append(encodePath(subpath));
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Purl other)) {
            return false;
        }
        return type.equals(other.type)
                && name.equals(other.name)
                && Objects.equals(namespace, other.namespace)
                && Objects.equals(version, other.version)
                && Objects.equals(subpath, other.subpath)
                && qualifiers.equals(other.qualifiers);
    }

    @Override
    public int hashCode() {
        int h = type.hashCode();
        h = 31 * h + (namespace != null ? namespace.hashCode() : 0);
        h = 31 * h + name.hashCode();
        h = 31 * h + (version != null ? version.hashCode() : 0);
        h = 31 * h + qualifiers.hashCode();
        h = 31 * h + (subpath != null ? subpath.hashCode() : 0);
        return h;
    }

    /**
     * Percent-encodes a string per RFC 3986. Only unreserved characters
     * ({@code A-Z a-z 0-9 . - _ ~}) are left unencoded; everything else
     * (including {@code : / @}) is percent-encoded.
     * <p>
     * Used for type, namespace segments, name, and version components.
     * For qualifier values use {@link #percentEncodeQualifierValue(String)}.
     */
    static String percentEncode(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        int i = 0;
        // As an optimization, scan characters looking for one that requires encoding.
        // If none is found the original string is returned as-is.
        while (i < input.length()) {
            char c = input.charAt(i);
            if (!isUnreserved(c)) {
                break;
            }
            i++;
        }
        if (i == input.length()) {
            return input;
        }
        StringBuilder sb = new StringBuilder(input.length() + 2);
        sb.append(input, 0, i);
        for (byte b : input.substring(i).getBytes(StandardCharsets.UTF_8)) {
            if (isUnreserved(b)) {
                sb.append((char) b);
            } else {
                appendPercentEncoded(sb, b);
            }
        }
        return sb.toString();
    }

    /**
     * Decodes percent-encoded triplets ({@code %XX}) back to characters.
     * Handles both encoded and unencoded input, so it correctly parses
     * qualifier values regardless of whether the producer encoded
     * {@code /} and {@code :} (Java implementations) or left them
     * unencoded (Python implementations, purl spec test suite).
     */
    static String percentDecode(String input) {
        // no percent signs means nothing to decode
        if (input == null || input.indexOf('%') < 0) {
            return input;
        }
        StringBuilder sb = new StringBuilder(input.length());
        int pos = 0;
        int pct;
        while ((pct = input.indexOf('%', pos)) >= 0) {
            sb.append(input, pos, pct);
            // collect consecutive %XX triplets into a byte[] for proper multi-byte UTF-8 decoding
            // (e.g. %C3%A9 -> two bytes -> one character é)
            int tripletStart = pct;
            int byteCount = 0;
            byte[] bytes = null;
            while (pct + 2 < input.length() && input.charAt(pct) == '%') {
                int hi = Character.digit(input.charAt(pct + 1), 16);
                int lo = Character.digit(input.charAt(pct + 2), 16);
                if (hi < 0 || lo < 0) {
                    break;
                }
                if (bytes == null) {
                    // each %XX triplet is 3 chars, so the max number of decoded bytes is the remaining length / 3
                    bytes = new byte[(input.length() - tripletStart) / 3];
                }
                bytes[byteCount++] = (byte) ((hi << 4) | lo);
                pct += 3;
            }
            if (byteCount > 0) {
                sb.append(new String(bytes, 0, byteCount, StandardCharsets.UTF_8));
                pos = pct;
            } else {
                throw new IllegalArgumentException(
                        "Invalid percent-encoding at index " + tripletStart + " in: " + input);
            }
        }
        sb.append(input, pos, input.length());
        return sb.toString();
    }

    /**
     * Percent-encodes each segment of a {@code /}-delimited path individually,
     * preserving literal {@code /} separators. A single pass determines whether
     * the path contains any {@code /} (i.e. is multi-segment) and whether any
     * character requires encoding, avoiding redundant scans.
     *
     * @param path the decoded path (namespace or subpath)
     * @return the encoded path with each segment percent-encoded
     */
    private static String encodePath(String path) {
        // Single pass: find the first '/' (to know if the path is multi-segment)
        // and detect whether any non-unreserved, non-'/' character exists
        // (to know if encoding is needed at all).
        int firstSlash = -1;
        boolean needsEncoding = false;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '/') {
                // Record only the first slash position; we'll use it to start
                // the segment-splitting loop below without re-scanning.
                if (firstSlash < 0) {
                    firstSlash = i;
                }
            } else if (!isUnreserved(c)) {
                // Found a character that requires percent-encoding.
                // Stop scanning — we know encoding is needed.
                needsEncoding = true;
                break;
            }
        }

        if (!needsEncoding) {
            // Every character is either unreserved or '/'.
            // Multi-segment: slashes are already literal, nothing to encode — return as-is.
            // Single-segment: delegate to percentEncode, which will also return as-is
            // (all chars are unreserved), but this keeps the single-segment path
            // through the same code path as callers that don't go through encodePath.
            return firstSlash < 0 ? percentEncode(path) : path;
        }

        if (firstSlash < 0) {
            // Needs encoding but has no slashes — single segment, delegate directly.
            return percentEncode(path);
        }

        // Multi-segment path that needs encoding: split on '/' and encode each
        // segment individually so that '/' separators remain unencoded.
        StringBuilder sb = new StringBuilder(path.length());
        int pos = 0;
        int slash = firstSlash;
        while (slash >= 0) {
            // Encode the segment before this slash and append the literal '/'.
            sb.append(percentEncode(path.substring(pos, slash))).append('/');
            pos = slash + 1;
            slash = path.indexOf('/', pos);
        }
        // Encode the final segment after the last slash.
        sb.append(percentEncode(path.substring(pos)));
        return sb.toString();
    }

    private static String decodePath(String path) {
        if (path.indexOf('%') < 0) {
            return path;
        }
        int slash = path.indexOf('/');
        if (slash < 0) {
            return percentDecode(path);
        }
        StringBuilder sb = new StringBuilder(path.length());
        int pos = 0;
        while (slash >= 0) {
            sb.append(percentDecode(path.substring(pos, slash))).append('/');
            pos = slash + 1;
            slash = path.indexOf('/', pos);
        }
        sb.append(percentDecode(path.substring(pos)));
        return sb.toString();
    }

    /**
     * Normalizes a subpath by discarding empty, {@code .}, and {@code ..} segments
     * as required by the purl spec. Returns {@code null} if the subpath is null,
     * empty, or consists entirely of discarded segments.
     *
     * @param subpath the raw subpath, or null
     * @return the normalized subpath, or null if nothing remains
     */
    private static String normalizeSubpath(String subpath) {
        if (subpath == null || subpath.isEmpty()) {
            return null;
        }
        StringBuilder sb = null;
        int segStart = 0;
        int len = subpath.length();
        for (int i = 0; i <= len; i++) {
            if (i == len || subpath.charAt(i) == '/') {
                int segLen = i - segStart;
                if (segLen == 0
                        || (segLen == 1 && subpath.charAt(segStart) == '.')
                        || (segLen == 2 && subpath.charAt(segStart) == '.' && subpath.charAt(segStart + 1) == '.')) {
                    if (sb == null) {
                        sb = new StringBuilder(len);
                        if (segStart > 0) {
                            sb.append(subpath, 0, segStart - 1);
                        }
                    }
                } else if (sb != null) {
                    if (!sb.isEmpty()) {
                        sb.append('/');
                    }
                    sb.append(subpath, segStart, i);
                }
                segStart = i + 1;
            }
        }
        if (sb == null) {
            return subpath;
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    /**
     * Percent-encodes a qualifier value per the purl spec.
     * <p>
     * In addition to the RFC 3986 unreserved set ({@code A-Z a-z 0-9 . - _ ~}),
     * {@code /} and {@code :} are left unencoded:
     * <ul>
     * <li>{@code :} — the spec states it must not be encoded "whether as a
     * separator or otherwise"</li>
     * <li>{@code /} — the official test suite expects unencoded slashes in
     * qualifier values (e.g. {@code repository_url=repo.acme.org/release})</li>
     * </ul>
     * <p>
     * {@code @} is still encoded because the spec only exempts it when used as
     * the name/version separator, not inside qualifier values.
     */
    static String percentEncodeQualifierValue(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        int i = 0;
        // As an optimization, scan characters looking for one that requires encoding first.
        // If none is found the original string is returned as-is.
        while (i < input.length()) {
            char c = input.charAt(i);
            if (!(isUnreserved(c) || c == '/' || c == ':')) {
                break;
            }
            i++;
        }
        if (i == input.length()) {
            return input;
        }
        StringBuilder sb = new StringBuilder(input.length() + 2);
        sb.append(input, 0, i);
        for (byte b : input.substring(i).getBytes(StandardCharsets.UTF_8)) {
            if (isUnreserved(b) || b == '/' || b == ':') {
                sb.append((char) b);
            } else {
                appendPercentEncoded(sb, b);
            }
        }
        return sb.toString();
    }

    private static void appendPercentEncoded(StringBuilder sb, byte b) {
        sb.append('%');
        int unsigned = b & 0xFF;
        sb.append(HEX_DIGITS[unsigned >> 4]);
        sb.append(HEX_DIGITS[unsigned & 0x0F]);
    }

    private static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();

    private static final boolean[] UNRESERVED = new boolean[128];
    static {
        for (char c = 'a'; c <= 'z'; c++) {
            UNRESERVED[c] = true;
        }
        for (char c = 'A'; c <= 'Z'; c++) {
            UNRESERVED[c] = true;
        }
        for (char c = '0'; c <= '9'; c++) {
            UNRESERVED[c] = true;
        }
        UNRESERVED['-'] = true;
        UNRESERVED['.'] = true;
        UNRESERVED['_'] = true;
        UNRESERVED['~'] = true;
    }

    private static void validateType(String type) {
        char first = type.charAt(0);
        if (!((first >= 'a' && first <= 'z') || (first >= 'A' && first <= 'Z'))) {
            throw new IllegalArgumentException("PURL type must start with a letter: " + type);
        }
        for (int i = 1; i < type.length(); i++) {
            char c = type.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '.' || c == '+' || c == '-')) {
                throw new IllegalArgumentException("PURL type contains invalid character '" + c + "': " + type);
            }
        }
    }

    private static boolean isUnreserved(int c) {
        return c >= 0 && c < 128 && UNRESERVED[c];
    }

    public static class Builder {

        private String type;
        private String namespace;
        private String name;
        private String version;
        private TreeMap<String, String> qualifiers;
        private String subpath;

        private Builder() {
        }

        public Builder setType(String type) {
            this.type = type;
            return this;
        }

        public Builder setNamespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setVersion(String version) {
            this.version = version;
            return this;
        }

        public Builder addQualifier(String key, String value) {
            if (qualifiers == null) {
                qualifiers = new TreeMap<>();
            }
            qualifiers.put(key, value);
            return this;
        }

        public Builder setQualifiers(Map<String, String> qualifiers) {
            this.qualifiers = qualifiers == null ? null : new TreeMap<>(qualifiers);
            return this;
        }

        public Builder setSubpath(String subpath) {
            this.subpath = subpath;
            return this;
        }

        public Purl build() {
            Map<String, String> q = qualifiers == null || qualifiers.isEmpty()
                    ? Map.of()
                    : Collections.unmodifiableMap(qualifiers);
            return new Purl(type, namespace, name, version, q, subpath);
        }
    }
}
