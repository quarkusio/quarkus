package io.quarkus.qute;

import java.util.HashMap;
import java.util.Map;

public class Scope {

    public static final Scope EMPTY = new Scope(null) {
        @Override
        public void putBinding(String binding, String type) {
            throw new UnsupportedOperationException("Immutable empty scope");
        }
    };

    private final Scope parentScope;
    private Map<String, String> bindings;
    private Map<String, Object> attributes;
    // TODO: add proper API to handle this
    private String lastPartHint;

    public Scope(Scope parentScope) {
        this.parentScope = parentScope;
    }

    public void putBinding(String binding, String type) {
        if (bindings == null)
            bindings = new HashMap<>();
        bindings.put(binding, type);
    }

    public String getBinding(String binding) {
        // we can contain null types to override outer scopes
        if (bindings != null
                && bindings.containsKey(binding))
            return bindings.get(binding);
        return parentScope != null ? parentScope.getBinding(binding) : null;
    }

    public String getBindingTypeOrDefault(String binding, String defaultValue) {
        String type = getBinding(binding);
        return type != null ? type : defaultValue;
    }

    public void putAttribute(String name, Object value) {
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        attributes.put(name, value);
    }

    public Object getAttribute(String name) {
        return attributes != null ? attributes.get(name) : null;
    }

    public String getLastPartHint() {
        return lastPartHint;
    }

    public void setLastPartHint(String lastPartHint) {
        this.lastPartHint = lastPartHint;
    }

}
