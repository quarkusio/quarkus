package io.quarkus.security.deployment;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Assigns the artificial configuration keys to the configuration expressions used in place of role names,
 * for example {@code @RolesAllowed("${admin-role}")}. The expressions are registered with the roles allowed
 * configuration source under these keys so that the Config system resolves them.
 */
final class RolesAllowedConfigExpressionKeys {

    private final Map<String, Integer> expressionToKey = new LinkedHashMap<>();
    private int nextKey;

    int keyFor(String configExpression) {
        Integer key = expressionToKey.get(configExpression);
        if (key == null) {
            key = nextKey++;
            expressionToKey.put(configExpression, key);
        }
        return key;
    }

    Map<Integer, String> keyToExpression() {
        Map<Integer, String> keyToExpression = new HashMap<>();
        for (Map.Entry<String, Integer> entry : expressionToKey.entrySet()) {
            keyToExpression.put(entry.getValue(), entry.getKey());
        }
        return Map.copyOf(keyToExpression);
    }
}
