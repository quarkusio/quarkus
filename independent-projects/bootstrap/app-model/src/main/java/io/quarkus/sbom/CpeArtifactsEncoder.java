package io.quarkus.sbom;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import io.quarkus.maven.dependency.ArtifactCoords;

/**
 * Encodes and decodes the {@code cpe-artifacts} platform property.
 * <p>
 * The property records, for each supported runtime extension artifact of a platform member, the set of
 * artifacts that a consumer (a Quarkus build step) should attribute to that member's CPE. The data is a
 * map keyed by the <em>runtime</em> extension artifact, so a consumer can look up the runtime artifacts
 * flagged in the {@code ApplicationModel} directly, without having to map them to their deployment
 * counterparts itself. The value of each entry is the extension's deployment artifact followed by the
 * artifacts of its resolved deployment dependency closure (aligned to the versions the platform ships).
 * <p>
 * <h2>Serialized format</h2>
 * The same coordinates recur heavily across entries (deployment closures overlap between extensions), and
 * even distinct coordinates share groupIds, versions and artifactId prefixes. The map is serialized in two
 * sections separated by a {@value #SEPARATOR} line:
 * <ol>
 * <li>a <em>dictionary</em> of every distinct coordinate, grouped so shared parts are written once;</li>
 * <li>the <em>entries</em>: one line per runtime extension as {@code <runtime-index>[<delta>,<delta>,...]},
 * where the runtime index and the (delta-encoded) dependency indices are base-36 references into the
 * dictionary.</li>
 * </ol>
 * <h3>Dictionary section</h3>
 * Coordinates are grouped by groupId; within a groupId they are grouped by version. Each line is
 * self-identifying by its first character:
 * <ul>
 * <li>{@code @groupId} &mdash; starts a group; the <em>very next line</em> is that group's common
 * artifactId prefix (possibly empty), applied to every artifact line in the group;</li>
 * <li>{@code =version} &mdash; starts a version sub-block within the current group;</li>
 * <li>anything else &mdash; an artifact line: the artifactId with the group prefix stripped, optionally
 * followed by {@code :classifier} and/or {@code :type}. Trailing defaults (empty classifier, {@code jar}
 * type) are omitted, so a plain artifactId means an empty-classifier {@code jar}. An <em>empty</em> line
 * means the artifactId equals the group prefix exactly.</li>
 * </ul>
 * Artifact lines take dictionary indices sequentially in the order they appear; the {@code @}/prefix/{@code =}
 * lines consume no index. The prefix line is positional (always present immediately after {@code @groupId}),
 * so no sentinel character is needed for it and empty lines are unambiguously artifact lines.
 * <p>
 * Removing the long-range repetition this way (which deflate's 32&nbsp;KB window cannot reach) and
 * delta-encoding the ascending dependency indices shrinks a real platform member's encoded value by roughly
 * a fifth beyond a naive dictionary.
 * <p>
 * The output is <strong>deterministic</strong> and therefore reproducible: the dictionary is sorted by
 * (groupId, version, artifactId, classifier, type), so index assignment depends only on content (not on the
 * input map's iteration order); entries are ordered by their runtime key's dictionary index; and dependency
 * indices are emitted in ascending order. The serialized text is then deflate-compressed and Base64-encoded
 * to keep the property value compact. Byte-identical output across builds additionally assumes a consistent
 * JDK/zlib (the release toolchain), as with any deflate-based artifact.
 */
public final class CpeArtifactsEncoder {

    /** Line separating the coordinate dictionary from the entry section. Never a valid dictionary line. */
    static final String SEPARATOR = "--";
    /** Radix used to encode dictionary indices compactly. */
    private static final int RADIX = 36;

    /** Total order over coordinates used for the dictionary; distinguishes every distinct coordinate. */
    private static final Comparator<ArtifactCoords> COORDS_ORDER = Comparator
            .comparing(ArtifactCoords::getGroupId)
            .thenComparing(ArtifactCoords::getVersion)
            .thenComparing(ArtifactCoords::getArtifactId)
            .thenComparing(ArtifactCoords::getClassifier)
            .thenComparing(ArtifactCoords::getType);

    private CpeArtifactsEncoder() {
    }

    public static String encode(Map<ArtifactCoords, List<ArtifactCoords>> deploymentDeps) {
        if (deploymentDeps.isEmpty()) {
            return "";
        }
        String text = serialize(deploymentDeps);
        byte[] compressed = deflate(text.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(compressed);
    }

    public static Map<ArtifactCoords, List<ArtifactCoords>> decode(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return Map.of();
        }
        byte[] compressed = Base64.getDecoder().decode(encoded);
        byte[] raw = inflate(compressed);
        String text = new String(raw, StandardCharsets.UTF_8);
        return deserialize(text);
    }

    static String serialize(Map<ArtifactCoords, List<ArtifactCoords>> deploymentDeps) {
        // Distinct coordinates, ordered deterministically so index assignment depends only on content.
        // The runtime key need not be added explicitly: by Quarkus extension convention a deployment
        // artifact depends on its runtime artifact, so every key appears in its own dependency closure.
        var distinct = new TreeSet<ArtifactCoords>(COORDS_ORDER);
        int depOccurrences = 0;
        for (var entry : deploymentDeps.entrySet()) {
            for (ArtifactCoords dep : entry.getValue()) {
                distinct.add(dep);
                depOccurrences++;
            }
        }

        var dictionary = new ArrayList<>(distinct);
        var index = new HashMap<ArtifactCoords, Integer>(dictionary.size());
        for (int i = 0; i < dictionary.size(); i++) {
            index.put(dictionary.get(i), i);
        }

        var sb = new StringBuilder(dictionary.size() * 16 + depOccurrences * 4 + 64);

        // Dictionary section: grouped by groupId, then by version, factoring out the common artifactId prefix.
        int n = dictionary.size();
        int i = 0;
        while (i < n) {
            String groupId = dictionary.get(i).getGroupId();
            int groupEnd = i;
            while (groupEnd < n && dictionary.get(groupEnd).getGroupId().equals(groupId)) {
                groupEnd++;
            }
            String prefix = commonArtifactIdPrefix(dictionary, i, groupEnd);
            sb.append('@').append(groupId).append('\n');
            sb.append(prefix).append('\n'); // positional prefix line, empty when there is no shared prefix
            String version = null;
            for (int k = i; k < groupEnd; k++) {
                ArtifactCoords c = dictionary.get(k);
                if (!c.getVersion().equals(version)) {
                    version = c.getVersion();
                    sb.append('=').append(version).append('\n');
                }
                appendRemainder(sb, c, prefix.length());
                sb.append('\n');
            }
            i = groupEnd;
        }
        sb.append(SEPARATOR).append('\n');

        // Entries: ordered by the key's dictionary index; dependency indices sorted ascending, then
        // delta-encoded (first index absolute, the rest as gaps) to keep the numbers small.
        var entries = new ArrayList<>(deploymentDeps.entrySet());
        entries.sort(Comparator.comparingInt(e -> index.get(e.getKey())));
        for (var entry : entries) {
            sb.append(Integer.toString(index.get(entry.getKey()), RADIX));
            sb.append('[');
            int[] depIndices = entry.getValue().stream().mapToInt(index::get).sorted().toArray();
            int prev = 0;
            for (int j = 0; j < depIndices.length; j++) {
                if (j > 0) {
                    sb.append(',');
                }
                sb.append(Integer.toString(depIndices[j] - prev, RADIX));
                prev = depIndices[j];
            }
            sb.append(']').append('\n');
        }
        return sb.toString();
    }

    static Map<ArtifactCoords, List<ArtifactCoords>> deserialize(String text) {
        String[] lines = text.split("\n", -1);

        // Section 1: the coordinate dictionary, up to the separator line. Empty lines are significant here
        // (an empty prefix line, or an artifact whose id equals the prefix), so blanks are not skipped.
        var dictionary = new ArrayList<ArtifactCoords>();
        int i = 0;
        boolean separatorSeen = false;
        String groupId = null;
        String prefix = "";
        String version = null;
        boolean awaitingPrefix = false;
        for (; i < lines.length; i++) {
            String line = lines[i];
            if (line.equals(SEPARATOR)) {
                separatorSeen = true;
                i++;
                break;
            }
            if (awaitingPrefix) {
                prefix = line;
                awaitingPrefix = false;
                continue;
            }
            if (!line.isEmpty() && line.charAt(0) == '@') {
                groupId = line.substring(1);
                prefix = "";
                version = null;
                awaitingPrefix = true;
                continue;
            }
            if (!line.isEmpty() && line.charAt(0) == '=') {
                version = line.substring(1);
                continue;
            }
            // Artifact line (possibly empty, meaning the artifactId equals the prefix).
            if (groupId == null || version == null) {
                throw new IllegalArgumentException("Invalid format: artifact before group/version: '" + line + "'");
            }
            dictionary.add(parseArtifact(groupId, prefix, version, line));
        }
        if (!separatorSeen) {
            throw new IllegalArgumentException("Invalid format: missing '" + SEPARATOR + "' separator");
        }

        // Section 2: the entries, referencing the dictionary by index (dependency indices delta-encoded).
        var result = new LinkedHashMap<ArtifactCoords, List<ArtifactCoords>>();
        for (; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty()) {
                continue;
            }
            int bracketOpen = line.indexOf('[');
            if (bracketOpen < 0 || !line.endsWith("]")) {
                throw new IllegalArgumentException("Invalid format: " + line);
            }
            var deploymentCoords = dictionary.get(parseIndex(line.substring(0, bracketOpen), dictionary));
            String depsStr = line.substring(bracketOpen + 1, line.length() - 1);
            List<ArtifactCoords> deps;
            if (depsStr.isEmpty()) {
                deps = List.of();
            } else {
                var parts = depsStr.split(",");
                deps = new ArrayList<>(parts.length);
                int running = 0;
                for (String part : parts) {
                    running += parseRadix(part);
                    deps.add(dictionary.get(checkIndex(running, dictionary)));
                }
            }
            result.put(deploymentCoords, deps);
        }
        return result;
    }

    /** Longest common prefix of the artifactIds in {@code dictionary[from, to)}. */
    private static String commonArtifactIdPrefix(List<ArtifactCoords> dictionary, int from, int to) {
        String prefix = dictionary.get(from).getArtifactId();
        for (int k = from + 1; k < to && !prefix.isEmpty(); k++) {
            String artifactId = dictionary.get(k).getArtifactId();
            int max = Math.min(prefix.length(), artifactId.length());
            int p = 0;
            while (p < max && prefix.charAt(p) == artifactId.charAt(p)) {
                p++;
            }
            prefix = prefix.substring(0, p);
        }
        return prefix;
    }

    /** Appends an artifact line: the artifactId beyond {@code prefixLen}, with trailing default parts omitted. */
    private static void appendRemainder(StringBuilder sb, ArtifactCoords c, int prefixLen) {
        String artifactId = c.getArtifactId();
        sb.append(artifactId, prefixLen, artifactId.length());
        String classifier = c.getClassifier();
        String type = c.getType();
        boolean jar = ArtifactCoords.TYPE_JAR.equals(type);
        if (classifier.isEmpty() && jar) {
            return;
        }
        if (jar) {
            sb.append(':').append(classifier);
        } else if (classifier.isEmpty()) {
            sb.append("::").append(type);
        } else {
            sb.append(':').append(classifier).append(':').append(type);
        }
    }

    private static ArtifactCoords parseArtifact(String groupId, String prefix, String version, String line) {
        String[] parts = line.split(":", -1);
        String artifactId = prefix + parts[0];
        String classifier;
        String type;
        switch (parts.length) {
            case 1:
                classifier = ArtifactCoords.DEFAULT_CLASSIFIER;
                type = ArtifactCoords.TYPE_JAR;
                break;
            case 2:
                classifier = parts[1];
                type = ArtifactCoords.TYPE_JAR;
                break;
            case 3:
                classifier = parts[1];
                type = parts[2];
                break;
            default:
                throw new IllegalArgumentException("Invalid artifact entry: " + line);
        }
        return ArtifactCoords.of(groupId, artifactId, classifier, type, version);
    }

    private static int parseIndex(String token, List<ArtifactCoords> dictionary) {
        return checkIndex(parseRadix(token), dictionary);
    }

    private static int parseRadix(String token) {
        try {
            return Integer.parseInt(token, RADIX);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid base-" + RADIX + " token: " + token, e);
        }
    }

    private static int checkIndex(int idx, List<ArtifactCoords> dictionary) {
        if (idx < 0 || idx >= dictionary.size()) {
            throw new IllegalArgumentException("Dictionary index out of range: " + idx);
        }
        return idx;
    }

    private static byte[] deflate(byte[] input) {
        var deflater = new Deflater(Deflater.BEST_COMPRESSION);
        try {
            deflater.setInput(input);
            deflater.finish();
            var out = new ByteArrayOutputStream(input.length);
            var buf = new byte[1024];
            while (!deflater.finished()) {
                int count = deflater.deflate(buf);
                out.write(buf, 0, count);
            }
            return out.toByteArray();
        } finally {
            deflater.end();
        }
    }

    private static byte[] inflate(byte[] input) {
        var inflater = new Inflater();
        try {
            inflater.setInput(input);
            var out = new ByteArrayOutputStream(input.length * 4);
            var buf = new byte[1024];
            while (!inflater.finished()) {
                if (inflater.needsInput() || inflater.needsDictionary()) {
                    throw new IllegalArgumentException("Truncated or corrupt cpe-artifacts data");
                }
                int count = inflater.inflate(buf);
                out.write(buf, 0, count);
            }
            return out.toByteArray();
        } catch (java.util.zip.DataFormatException e) {
            throw new IllegalArgumentException("Failed to inflate cpe-artifacts data", e);
        } finally {
            inflater.end();
        }
    }
}
