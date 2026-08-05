package io.quarkus.mongodb.panache.common.binder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.conversions.Bson;

public class NativeQueryBinder {

    private static final String PLACEHOLDER_PREFIX = "__PANACHE_PARAM_";
    private static final String PLACEHOLDER_SUFFIX = "__";

    public static Bson bindQuery(String query, Object[] params) {
        Map<String, Object> placeholderValues = new HashMap<>();
        String boundQuery = query;
        for (int i = 1; i <= params.length; i++) {
            String bindParamsKey = "?" + i;
            String placeholder = toPlaceholder(bindParamsKey);
            placeholderValues.put(placeholder, CommonQueryBinder.paramValue(params[i - 1]));
            boundQuery = boundQuery.replace(bindParamsKey, "'" + placeholder + "'");
        }

        Document document = Document.parse(boundQuery);
        replacePlaceholders(document, placeholderValues);
        return document;
    }

    public static Bson bindQuery(String query, Map<String, Object> params) {
        Map<String, Object> placeholderValues = new HashMap<>();
        String boundQuery = query;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String bindParamsKey = ":" + entry.getKey();
            String placeholder = toPlaceholder(bindParamsKey);
            placeholderValues.put(placeholder, CommonQueryBinder.paramValue(entry.getValue()));
            boundQuery = boundQuery.replace(bindParamsKey, "'" + placeholder + "'");
        }

        Document document = Document.parse(boundQuery);
        replacePlaceholders(document, placeholderValues);
        return document;
    }

    private static String toPlaceholder(String key) {
        return PLACEHOLDER_PREFIX + key + PLACEHOLDER_SUFFIX;
    }

    @SuppressWarnings("unchecked")
    private static void replacePlaceholders(Document document, Map<String, Object> placeholderValues) {
        for (Map.Entry<String, Object> entry : document.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String && placeholderValues.containsKey(value)) {
                document.put(entry.getKey(), placeholderValues.get(value));
            } else if (value instanceof Document) {
                replacePlaceholders((Document) value, placeholderValues);
            } else if (value instanceof List) {
                replaceInList((List<Object>) value, placeholderValues);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void replaceInList(List<Object> list, Map<String, Object> placeholderValues) {
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            if (item instanceof String && placeholderValues.containsKey(item)) {
                list.set(i, placeholderValues.get(item));
            } else if (item instanceof Document) {
                replacePlaceholders((Document) item, placeholderValues);
            } else if (item instanceof List) {
                replaceInList((List<Object>) item, placeholderValues);
            }
        }
    }
}
