package io.quarkus.funqy.runtime.query;

import java.util.List;
import java.util.Map;

interface QueryPropertySetter {
    Object create();

    void setValue(Object target, String name, String value, Map<String, List<Object>> paramToObject);
}
