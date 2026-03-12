package io.quarkus.devshell.runtime.tui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lightweight JSON parsing for JSON-RPC response extraction.
 */
public final class JsonHelper {

    private JsonHelper() {
    }

    public static String getString(String json, String key) {
        int valueStart = findValueStart(json, key);
        if (valueStart < 0 || valueStart >= json.length() || json.charAt(valueStart) != '"') {
            return null;
        }

        return extractQuotedString(json, valueStart);
    }

    public static int getInt(String json, String key, int defaultValue) {
        double d = getDouble(json, key);
        return Double.isNaN(d) ? defaultValue : (int) d;
    }

    public static int getInt(String json, String key) {
        return getInt(json, key, 0);
    }

    public static long getLong(String json, String key, long defaultValue) {
        double d = getDouble(json, key);
        return Double.isNaN(d) ? defaultValue : (long) d;
    }

    public static double getDouble(String json, String key) {
        int valueStart = findValueStart(json, key);
        if (valueStart < 0) {
            return Double.NaN;
        }

        int valueEnd = valueStart;
        while (valueEnd < json.length()) {
            char c = json.charAt(valueEnd);
            if (!Character.isDigit(c) && c != '.' && c != '-' && c != 'E' && c != 'e' && c != '+') {
                break;
            }
            valueEnd++;
        }

        if (valueEnd == valueStart) {
            return Double.NaN;
        }

        try {
            return Double.parseDouble(json.substring(valueStart, valueEnd));
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    public static boolean getBoolean(String json, String key, boolean defaultValue) {
        int valueStart = findValueStart(json, key);
        if (valueStart < 0 || valueStart >= json.length()) {
            return defaultValue;
        }

        if (json.startsWith("true", valueStart)) {
            return true;
        } else if (json.startsWith("false", valueStart)) {
            return false;
        }
        return defaultValue;
    }

    public static boolean getBoolean(String json, String key) {
        return getBoolean(json, key, false);
    }

    public static String getObject(String json, String key) {
        int valueStart = findValueStart(json, key);
        if (valueStart < 0 || valueStart >= json.length() || json.charAt(valueStart) != '{') {
            return null;
        }

        int endIdx = findMatchingBrace(json, valueStart);
        if (endIdx == -1) {
            return null;
        }

        return json.substring(valueStart, endIdx + 1);
    }

    public static String getArray(String json, String key) {
        int valueStart = findValueStart(json, key);
        if (valueStart < 0 || valueStart >= json.length() || json.charAt(valueStart) != '[') {
            return null;
        }

        int endIdx = findMatchingBracket(json, valueStart);
        if (endIdx == -1) {
            return null;
        }

        return json.substring(valueStart, endIdx + 1);
    }

    public static String getNestedString(String json, String parentKey, String childKey) {
        String nested = getObject(json, parentKey);
        if (nested == null) {
            return null;
        }
        return getString(nested, childKey);
    }

    public static int findMatchingBrace(String json, int startIdx) {
        if (json == null || startIdx >= json.length() || json.charAt(startIdx) != '{') {
            return -1;
        }
        return findMatchingChar(json, startIdx, '{', '}');
    }

    public static int findMatchingBracket(String json, int startIdx) {
        if (json == null || startIdx >= json.length() || json.charAt(startIdx) != '[') {
            return -1;
        }
        return findMatchingChar(json, startIdx, '[', ']');
    }

    public static List<String> iterateArray(String jsonArray) {
        if (jsonArray == null || jsonArray.isEmpty() || jsonArray.charAt(0) != '[') {
            return Collections.emptyList();
        }

        List<String> elements = new ArrayList<>();
        int idx = 1;

        while (idx < jsonArray.length()) {
            idx = skipWhitespace(jsonArray, idx);

            if (idx >= jsonArray.length() || jsonArray.charAt(idx) == ']') {
                break;
            }

            if (jsonArray.charAt(idx) == ',') {
                idx++;
                continue;
            }

            char c = jsonArray.charAt(idx);
            if (c == '{') {
                int endIdx = findMatchingBrace(jsonArray, idx);
                if (endIdx == -1) {
                    break;
                }
                elements.add(jsonArray.substring(idx, endIdx + 1));
                idx = endIdx + 1;
            } else if (c == '[') {
                int endIdx = findMatchingBracket(jsonArray, idx);
                if (endIdx == -1) {
                    break;
                }
                elements.add(jsonArray.substring(idx, endIdx + 1));
                idx = endIdx + 1;
            } else if (c == '"') {
                String value = extractQuotedString(jsonArray, idx);
                if (value != null) {
                    elements.add(value);
                    idx = findClosingQuote(jsonArray, idx + 1) + 1;
                } else {
                    idx++;
                }
            } else {
                int valueEnd = idx;
                while (valueEnd < jsonArray.length()) {
                    char vc = jsonArray.charAt(valueEnd);
                    if (vc == ',' || vc == ']' || Character.isWhitespace(vc)) {
                        break;
                    }
                    valueEnd++;
                }
                elements.add(jsonArray.substring(idx, valueEnd));
                idx = valueEnd;
            }
        }

        return elements;
    }

    /**
     * Extracts the "result" field from a JSON-RPC response envelope.
     */
    public static String extractResult(String jsonRpcResponse) {
        if (jsonRpcResponse == null || jsonRpcResponse.isEmpty()) {
            return null;
        }

        int resultIdx = jsonRpcResponse.indexOf("\"result\"");
        if (resultIdx == -1) {
            return null;
        }

        int colonIdx = findColon(jsonRpcResponse, resultIdx + 8);
        if (colonIdx == -1) {
            return null;
        }

        int valueStart = skipWhitespace(jsonRpcResponse, colonIdx + 1);
        if (valueStart >= jsonRpcResponse.length()) {
            return null;
        }

        char startChar = jsonRpcResponse.charAt(valueStart);

        if (startChar == '[') {
            int endIdx = findMatchingBracket(jsonRpcResponse, valueStart);
            if (endIdx != -1) {
                return jsonRpcResponse.substring(valueStart, endIdx + 1);
            }
        } else if (startChar == '{') {
            int endIdx = findMatchingBrace(jsonRpcResponse, valueStart);
            if (endIdx != -1) {
                return jsonRpcResponse.substring(valueStart, endIdx + 1);
            }
        } else if (startChar == '"') {
            return extractQuotedString(jsonRpcResponse, valueStart);
        } else {
            int end = valueStart;
            while (end < jsonRpcResponse.length()) {
                char c = jsonRpcResponse.charAt(end);
                if (c == ',' || c == '}' || Character.isWhitespace(c)) {
                    break;
                }
                end++;
            }
            return jsonRpcResponse.substring(valueStart, end);
        }

        return null;
    }

    public static String extractError(String jsonRpcResponse) {
        if (jsonRpcResponse == null || !jsonRpcResponse.contains("\"error\"")) {
            return null;
        }
        return getNestedString(jsonRpcResponse, "error", "message");
    }

    /**
     * Finds the start index of the value for a given key in a JSON string.
     * Returns -1 if the key is not found or the JSON is null.
     */
    private static int findValueStart(String json, String key) {
        if (json == null || key == null) {
            return -1;
        }

        String searchKey = "\"" + key + "\"";
        int keyIdx = json.indexOf(searchKey);
        if (keyIdx == -1) {
            return -1;
        }

        int colonIdx = findColon(json, keyIdx + searchKey.length());
        if (colonIdx == -1) {
            return -1;
        }

        return skipWhitespace(json, colonIdx + 1);
    }

    private static int findColon(String json, int fromIdx) {
        int idx = skipWhitespace(json, fromIdx);
        if (idx < json.length() && json.charAt(idx) == ':') {
            return idx;
        }
        return -1;
    }

    private static int skipWhitespace(String json, int fromIdx) {
        while (fromIdx < json.length() && Character.isWhitespace(json.charAt(fromIdx))) {
            fromIdx++;
        }
        return fromIdx;
    }

    private static String extractQuotedString(String json, int quoteStart) {
        if (quoteStart >= json.length() || json.charAt(quoteStart) != '"') {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        int i = quoteStart + 1;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                switch (next) {
                    case '"':
                        sb.append('"');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case '/':
                        sb.append('/');
                        break;
                    default:
                        sb.append('\\');
                        sb.append(next);
                        break;
                }
                i += 2;
            } else if (c == '"') {
                return sb.toString();
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    private static int findClosingQuote(String json, int fromIdx) {
        int i = fromIdx;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\') {
                i += 2;
            } else if (c == '"') {
                return i;
            } else {
                i++;
            }
        }
        return -1;
    }

    private static int findMatchingChar(String json, int startIdx, char open, char close) {
        int depth = 1;
        int idx = startIdx + 1;
        boolean inString = false;
        while (idx < json.length() && depth > 0) {
            char c = json.charAt(idx);
            if (inString) {
                if (c == '\\') {
                    idx++;
                } else if (c == '"') {
                    inString = false;
                }
            } else {
                if (c == '"') {
                    inString = true;
                } else if (c == open) {
                    depth++;
                } else if (c == close) {
                    depth--;
                }
            }
            idx++;
        }

        return depth == 0 ? idx - 1 : -1;
    }
}
