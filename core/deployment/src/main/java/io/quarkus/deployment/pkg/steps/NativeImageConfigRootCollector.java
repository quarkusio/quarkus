package io.quarkus.deployment.pkg.steps;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.stream.JsonParser;

import org.jboss.logging.Logger;

import io.quarkus.paths.OpenPathTree;
import io.quarkus.paths.PathTree;

/**
 * Collects tree-shake roots from {@code META-INF/native-image/} configuration files.
 * <p>
 * Classes referenced in {@code reflect-config.json} and {@code jni-config.json} are roots in all modes,
 * since they describe dynamic access performed by the library at runtime.
 * Classes referenced in {@code native-image.properties} ({@code --initialize-at-run-time},
 * {@code --initialize-at-build-time} and {@code --features}) are only meaningful to {@code native-image},
 * so these files are read only when building a native executable.
 */
final class NativeImageConfigRootCollector {

    private static final Logger log = Logger.getLogger(NativeImageConfigRootCollector.class);

    private static final String META_INF_NATIVE_IMAGE = "META-INF/native-image/";

    private static final String[] CLASS_LIST_ARGS = {
            "--initialize-at-run-time=",
            "--initialize-at-build-time=",
            "--features="
    };

    private NativeImageConfigRootCollector() {
    }

    /**
     * Collects roots from the native-image configuration files found in the given tree.
     *
     * @param tree content tree to scan
     * @param nativeTarget whether a native executable is being built
     * @param roots receives the dot-separated names of root classes, possibly more than once
     */
    static void collect(PathTree tree, boolean nativeTarget, Consumer<String> roots) {
        try (OpenPathTree openTree = tree.open()) {
            openTree.walkIfContains(META_INF_NATIVE_IMAGE, visit -> {
                String path = visit.getResourceName();
                if (path.endsWith("native-image.properties")) {
                    if (nativeTarget) {
                        parseNativeImageProperties(visit.getPath(), roots);
                    }
                } else if (path.endsWith("reflect-config.json") || path.endsWith("jni-config.json")) {
                    parseJsonClassConfig(visit.getPath(), roots);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read content of " + tree.getRoots(), e);
        }
    }

    /**
     * Parses a {@code native-image.properties} file and extracts class names from
     * {@code --initialize-at-run-time}, {@code --initialize-at-build-time}, and
     * {@code --features} arguments.
     * Lines starting with {@code #} are comments. A trailing {@code \} continues a line,
     * so an argument value never spans lines.
     */
    private static void parseNativeImageProperties(Path file, Consumer<String> roots) {
        final String content;
        try {
            content = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.debugf(e, "Failed to read native-image.properties: %s", file);
            return;
        }
        int lineStart = 0;
        while (lineStart < content.length()) {
            int lineEnd = content.indexOf('\n', lineStart);
            if (lineEnd < 0) {
                lineEnd = content.length();
            }
            parseNativeImagePropertiesLine(content, lineStart, lineEnd, roots);
            lineStart = lineEnd + 1;
        }
    }

    private static void parseNativeImagePropertiesLine(String content, int start, int end, Consumer<String> roots) {
        start = skipWhitespace(content, start, end);
        if (start == end || content.charAt(start) == '#') {
            return;
        }
        while (end > start && Character.isWhitespace(content.charAt(end - 1))) {
            end--;
        }
        if (start == end) {
            return;
        }
        // handle line continuations
        if (content.charAt(end - 1) == '\\') {
            end--;
        }
        for (String arg : CLASS_LIST_ARGS) {
            int idx = start;
            while ((idx = content.indexOf(arg, idx)) >= 0 && idx < end) {
                idx += arg.length();
                int valueEnd = idx;
                while (valueEnd < end && !Character.isWhitespace(content.charAt(valueEnd))) {
                    valueEnd++;
                }
                addCommaSeparatedNames(content, idx, valueEnd, roots);
                idx = valueEnd;
            }
        }
    }

    /**
     * Adds the comma-separated names in the given range, which contains no whitespace.
     */
    private static void addCommaSeparatedNames(String content, int start, int end, Consumer<String> roots) {
        while (start < end) {
            int comma = content.indexOf(',', start);
            int nameEnd = comma < 0 || comma > end ? end : comma;
            if (nameEnd > start) {
                roots.accept(content.substring(start, nameEnd));
            }
            start = nameEnd + 1;
        }
    }

    private static int skipWhitespace(String content, int start, int end) {
        while (start < end && Character.isWhitespace(content.charAt(start))) {
            start++;
        }
        return start;
    }

    /**
     * Parses a {@code reflect-config.json} or {@code jni-config.json} file and
     * extracts the {@code name} of each entry of the top-level array.
     * Files whose top level is not an array are ignored.
     */
    private static void parseJsonClassConfig(Path file, Consumer<String> roots) {
        try (InputStream is = Files.newInputStream(file);
                JsonParser parser = Json.createParser(is)) {
            if (!parser.hasNext() || parser.next() != JsonParser.Event.START_ARRAY) {
                return;
            }
            // depth 1 is the top-level array, depth 2 an entry object
            int depth = 1;
            while (depth > 0 && parser.hasNext()) {
                switch (parser.next()) {
                    case START_ARRAY, START_OBJECT -> depth++;
                    case END_ARRAY, END_OBJECT -> depth--;
                    case KEY_NAME -> {
                        if (depth == 2 && "name".equals(parser.getString())) {
                            JsonParser.Event value = parser.next();
                            if (value == JsonParser.Event.VALUE_STRING) {
                                String name = parser.getString();
                                if (!name.isEmpty()) {
                                    roots.accept(name);
                                }
                            } else if (value == JsonParser.Event.START_ARRAY || value == JsonParser.Event.START_OBJECT) {
                                depth++;
                            }
                        }
                    }
                    default -> {
                    }
                }
            }
        } catch (IOException | JsonException e) {
            log.debugf(e, "Failed to read native-image config: %s", file);
        }
    }
}
