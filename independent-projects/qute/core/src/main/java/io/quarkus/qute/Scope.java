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
        if (bindings == null) {
            bindings = new HashMap<>();
        }
        bindings.put(binding, sanitizeType(type));
    }

    public String getBinding(String binding) {
        // we can contain null types to override outer scopes
        if (bindings != null
                && bindings.containsKey(binding)) {
            return bindings.get(binding);
        }
        return parentScope != null ? parentScope.getBinding(binding) : null;
    }

    Map<String, String> getBindings() {
        return bindings;
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

    private String sanitizeType(String type) {
        if (type != null && type.contains("?")) {
            String upperBound = " extends ";
            String lowerBound = " super ";
            // ignore wildcards
            if (type.contains(upperBound)) {
                // upper bound
                type = type.replace("?", "").replace(upperBound, "").trim();
            } else if (type.contains(lowerBound)) {
                // lower bound
                type = type.replace("?", "").replace(lowerBound, "").trim();
            } else {
                // no bounds
                type = type.replace("?", "java.lang.Object");
            }
        }
        return type;
    }

}
