package io.quarkus.qute;

import java.util.HashMap;
import java.util.Map;

public abstract class TemplateInstanceBase implements TemplateInstance {

    protected Object data;
    protected Map<String, Object> dataMap;
    protected final Map<String, Object> attributes;

    public TemplateInstanceBase() {
        this.attributes = new HashMap<>();
    }

    @Override
    public TemplateInstance data(Object data) {
        this.data = data;
        dataMap = null;
        return this;
    }

    @Override
    public TemplateInstance data(String key, Object data) {
        this.data = null;
        if (dataMap == null) {
            dataMap = new HashMap<String, Object>();
        }
        dataMap.put(key, data);
        return this;
    }

    @Override
    public TemplateInstance setAttribute(String key, Object value) {
        attributes.put(key, value);
        return this;
    }

    @Override
    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    protected Object data() {
        return data != null ? data : dataMap;
    }

}
