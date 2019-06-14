package io.quarkus.amazon.lambda.resteasy.adapter.test.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Request/response model
 */
public class MapResponseModel {
    private Map<String, String> values;

    public MapResponseModel() {
        this.values = new HashMap<>();
    }

    public void addValue(String key, String value) {
        this.values.put(key, value);
    }

    public Map<String, String> getValues() {
        return values;
    }

    public void setValues(Map<String, String> values) {
        this.values = values;
    }
}