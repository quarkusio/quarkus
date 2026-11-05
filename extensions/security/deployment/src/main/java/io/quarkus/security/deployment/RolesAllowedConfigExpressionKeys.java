package io.quarkus.security.deployment;

import java.util.ArrayList;
import java.util.List;

/**
 * Assigns the artificial configuration keys to the configuration expressions used in place of role names,
 * for example {@code @RolesAllowed("${admin-role}")}. The expressions are registered with the roles allowed
 * configuration source under these keys so that the Config system resolves them.
 * <p>
 * The key of an expression is its index in the list of the expressions, each expression is listed exactly once.
 */
final class RolesAllowedConfigExpressionKeys {

    private final List<String> expressions = new ArrayList<>();

    int keyFor(String configExpression) {
        int key = expressions.indexOf(configExpression);
        if (key < 0) {
            key = expressions.size();
            expressions.add(configExpression);
        }
        return key;
    }

    /**
     * @return the expressions ordered by their keys, that is, the key of an expression is its index in the list
     */
    List<String> expressions() {
        return List.copyOf(expressions);
    }
}
