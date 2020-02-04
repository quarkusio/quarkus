package io.quarkus.qute.runtime;

import static io.quarkus.qute.TemplateExtension.ANY;

import java.util.List;
import java.util.Map;

import io.quarkus.qute.Results.Result;
import io.quarkus.qute.TemplateExtension;

@TemplateExtension
public class BuiltinTemplateExtensions {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @TemplateExtension(matchName = ANY)
    static Object map(Map map, String name) {
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
            default:
                return map.getOrDefault(name, Result.NOT_FOUND);
        }
    }

    static Object get(Map<?, ?> map, Object key) {
        return map.get(key);
    }

    static boolean containsKey(Map<?, ?> map, Object key) {
        return map.containsKey(key);
    }

    static Object get(List<?> list, int index) {
        return list.get(index);
    }

}
