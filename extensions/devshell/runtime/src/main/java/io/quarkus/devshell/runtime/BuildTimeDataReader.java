package io.quarkus.devshell.runtime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.devui.runtime.DevUIBuildTimeStaticService;

/**
 * Service to read build-time data from DevUI's static files.
 * This allows DevShell to access the same data that DevUI uses.
 */
@Singleton
public class BuildTimeDataReader {

    private static final Logger LOG = Logger.getLogger(BuildTimeDataReader.class);
    private static final String DASH_DATA_DOT_JS = "-data.js";

    @Inject
    DevUIBuildTimeStaticService buildTimeStaticService;

    /**
     * Get all build-time data for a given extension namespace.
     *
     * @param namespace the extension namespace (e.g., "quarkus-arc")
     * @return a map of field names to their JSON string values
     */
    public Map<String, String> getBuildTimeData(String namespace) {
        Map<String, String> urlAndPath = buildTimeStaticService.getUrlAndPath();
        String dataFileName = namespace + DASH_DATA_DOT_JS;

        for (Map.Entry<String, String> entry : urlAndPath.entrySet()) {
            if (entry.getKey().endsWith(dataFileName)) {
                try {
                    String content = Files.readString(Paths.get(entry.getValue()));
                    return parseBuildTimeDataFile(content);
                } catch (IOException e) {
                    LOG.warnf("Failed to read build-time data file for %s: %s", namespace, e.getMessage());
                    return Collections.emptyMap();
                }
            }
        }

        LOG.debugf("No build-time data file found for namespace: %s", namespace);
        return Collections.emptyMap();
    }

    /**
     * Get a specific build-time data field for an extension.
     *
     * @param namespace the extension namespace (e.g., "quarkus-arc")
     * @param fieldName the field name (e.g., "beans")
     * @return the JSON string value, or null if not found
     */
    public String getBuildTimeDataField(String namespace, String fieldName) {
        Map<String, String> data = getBuildTimeData(namespace);
        return data.get(fieldName);
    }

    /**
     * Parse a build-time data JavaScript file and extract the exported constants.
     * Files contain exports like: export const beans = [...];
     */
    private Map<String, String> parseBuildTimeDataFile(String content) {
        Map<String, String> result = new HashMap<>();

        // Pattern to match: export const <name> = <value>;
        // The value can be an array [...], object {...}, or primitive
        Pattern pattern = Pattern.compile("export\\s+const\\s+(\\w+)\\s*=\\s*", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String fieldName = matcher.group(1);
            int valueStart = matcher.end();

            // Find the value - it could be [...], {...}, "...", or a number/boolean
            if (valueStart < content.length()) {
                char startChar = content.charAt(valueStart);
                String value = null;

                if (startChar == '[') {
                    value = extractBracketedValue(content, valueStart, '[', ']');
                } else if (startChar == '{') {
                    value = extractBracketedValue(content, valueStart, '{', '}');
                } else if (startChar == '"' || startChar == '\'') {
                    value = extractStringValue(content, valueStart, startChar);
                } else {
                    // Primitive value - read until semicolon
                    int endIdx = content.indexOf(';', valueStart);
                    if (endIdx > valueStart) {
                        value = content.substring(valueStart, endIdx).trim();
                    }
                }

                if (value != null) {
                    result.put(fieldName, value);
                }
            }
        }

        return result;
    }

    private String extractBracketedValue(String content, int start, char openBracket, char closeBracket) {
        int depth = 0;
        boolean inString = false;
        char stringChar = 0;
        boolean escaped = false;

        for (int i = start; i < content.length(); i++) {
            char c = content.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\') {
                escaped = true;
                continue;
            }

            if (inString) {
                if (c == stringChar) {
                    inString = false;
                }
                continue;
            }

            if (c == '"' || c == '\'') {
                inString = true;
                stringChar = c;
                continue;
            }

            if (c == openBracket) {
                depth++;
            } else if (c == closeBracket) {
                depth--;
                if (depth == 0) {
                    return content.substring(start, i + 1);
                }
            }
        }

        return null;
    }

    private String extractStringValue(String content, int start, char quote) {
        boolean escaped = false;

        for (int i = start + 1; i < content.length(); i++) {
            char c = content.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\') {
                escaped = true;
                continue;
            }

            if (c == quote) {
                return content.substring(start, i + 1);
            }
        }

        return null;
    }
}
