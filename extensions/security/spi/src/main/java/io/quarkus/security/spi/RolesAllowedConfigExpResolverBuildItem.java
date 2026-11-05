package io.quarkus.security.spi;

import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Provides a way to transform roles allowed specified as configuration expressions in annotations to runtime
 * configuration values.
 */
public final class RolesAllowedConfigExpResolverBuildItem extends MultiBuildItem {
    private final String roleConfigExpr;

    /**
     * @param roleConfigExpr configuration expression used in place of a role name
     */
    public RolesAllowedConfigExpResolverBuildItem(String roleConfigExpr) {
        this.roleConfigExpr = Objects.requireNonNull(roleConfigExpr);
    }

    public String getRoleConfigExpr() {
        return roleConfigExpr;
    }

    public static boolean isSecurityConfigExpressionCandidate(String configExpression) {
        if (configExpression == null || configExpression.length() < 4) {
            return false;
        }
        final int exprStart = configExpression.indexOf("${");
        return exprStart >= 0 && configExpression.indexOf('}', exprStart + 2) > 0;
    }
}
