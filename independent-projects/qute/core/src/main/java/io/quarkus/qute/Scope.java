package io.quarkus.qute;

import java.util.HashMap;
import java.util.Map;

public class Scope {

    public static final Scope EMPTY = new Scope(null) {
        @Override
        public void put(String binding, String type) {
            throw new UnsupportedOperationException("Immutable empty scope");
        }
    };

    private Scope parentScope;
    private Map<String, String> bindings;

    public Scope(Scope parentScope) {
        this.parentScope = parentScope;
    }

    public void put(String binding, String type) {
        if (bindings == null)
            bindings = new HashMap<>();
        bindings.put(binding, type);
    }

    public String getBindingType(String binding) {
        // we can contain null types to override outer scopes
        if (bindings != null
                && bindings.containsKey(binding))
            return bindings.get(binding);
        return parentScope != null ? parentScope.getBindingType(binding) : null;
    }

    public String getBindingTypeOrDefault(String binding, String defaultValue) {
        String type = getBindingType(binding);
        return type != null ? type : defaultValue;
    }

}
