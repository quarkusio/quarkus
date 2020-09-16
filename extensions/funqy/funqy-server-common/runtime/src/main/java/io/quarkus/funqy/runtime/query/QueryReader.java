package io.quarkus.funqy.runtime.query;

import java.util.Iterator;
import java.util.Map;

public interface QueryReader<T> {
    T readValue(Iterator<Map.Entry<String, String>> params);
}
