package io.quarkus.qute.runtime;

import static io.quarkus.qute.TemplateExtension.ANY;

import java.util.Map;

import io.quarkus.qute.Results.Result;
import io.quarkus.qute.TemplateExtension;

public class DefaultTemplateExtensions {

    @SuppressWarnings("rawtypes")
    @TemplateExtension(matchName = ANY)
    public static Object map(Map map, String name) {
        Object val = map.get(name);
        if (val != null) {
            return val;
        }
        switch (name) {
            case "keys":
            case "keySet":
                return map.keySet();
            case "values":
                return map.values();
            case "size":
                return map.size();
            case "empty":
            case "isEmpty":
                return map.isEmpty();
            case "get":
                return map.get(name);
            case "containsKey":
                return map.containsKey(name);
            default:
                return Result.NOT_FOUND;
        }
    }

}
