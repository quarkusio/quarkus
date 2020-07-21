package io.quarkus.funqy.runtime.query;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public abstract class BaseObjectReader implements QueryReader<Object>, QueryPropertySetter {
    interface ValueSetter {
        void setValue(Object target, String propName, Object value);

        Function<String, Object> getExtractor();

        QueryPropertySetter getSetter();
    }

    @Override
    public Object readValue(Iterator<Map.Entry<String, String>> params) {
        Object target = create();
        // exists to track nested non-primitive objects
        Map<String, List<Object>> paramToObject = new HashMap<>();

        while (params.hasNext()) {
            Map.Entry<String, String> entry = params.next();
            String name = entry.getKey();
            String value = entry.getValue();
            setValue(target, name, value, paramToObject);
        }

        return target;
    }

    abstract ValueSetter getValueSetter(String propName);

    @Override
    public void setValue(Object target, String name, String value, Map<String, List<Object>> paramToObject) {
        try {
            if (name == null)
                return;
            String propName = name;
            String suffix = null;
            int dot = propName.indexOf('.');
            if (dot == 0)
                return;
            else if (dot > 0) {
                propName = name.substring(0, dot);
                suffix = name.substring(dot + 1);
            }
            ValueSetter setter = getValueSetter(propName);
            if (setter == null)
                return;
            if (suffix != null && setter.getSetter() == null)
                return;
            if (setter.getExtractor() != null) {
                Object val = setter.getExtractor().apply(value);
                setter.setValue(target, propName, val);
            } else if (setter.getSetter() != null) {
                List<Object> propEntry = paramToObject.get(propName);
                Object obj;
                Map<String, List<Object>> paramEntries;
                if (propEntry == null) {
                    obj = setter.getSetter().create();
                    paramEntries = new HashMap<>();
                    propEntry = Arrays.asList(obj, paramEntries);
                    paramToObject.put(propName, propEntry);
                    setter.setValue(target, propName, obj);
                } else {
                    obj = propEntry.get(0);
                    paramEntries = (Map<String, List<Object>>) propEntry.get(1);
                }
                setter.getSetter().setValue(obj, suffix, value, paramEntries);
            } else {
                // throw Exception?
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
