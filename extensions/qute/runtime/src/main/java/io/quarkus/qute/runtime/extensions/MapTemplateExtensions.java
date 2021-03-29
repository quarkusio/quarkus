package io.quarkus.qute.runtime.extensions;

import static io.quarkus.qute.TemplateExtension.ANY;

import java.util.Map;

import javax.enterprise.inject.Vetoed;

import io.quarkus.qute.Results.Result;
import io.quarkus.qute.TemplateExtension;

@Vetoed // Make sure no bean is created from this class
@TemplateExtension
public class MapTemplateExtensions {
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @TemplateExtension(matchName = ANY)
    static Object map(Map map, String name) {
        switch (name) {
            case "keys":
            case "keySet":
                return map.keySet();
            case "values":
                return map.values();
            case "entrySet":
                return map.entrySet();
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

}
