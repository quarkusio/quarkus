package io.quarkus.security.spi;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Provides a way to transform roles allowed specified as configuration expressions in annotations to runtime
 * configuration values.
 */
public final class RolesAllowedConfigExpResolverBuildItem extends MultiBuildItem {
    private final String roleConfigExpr;
    private final BiConsumer<String, Supplier<String[]>> configValueRecorder;

    /**
     * @param roleConfigExpr
     *        roles allowed configuration expression
     * @param configValueRecorder
     *        roles allowed supplier will be recorded to this consumer created during static-init; runtime roles
     *        allowed expressions are supplied correctly only when runtime config is ready
     */
    public RolesAllowedConfigExpResolverBuildItem(String roleConfigExpr,
            BiConsumer<String, Supplier<String[]>> configValueRecorder) {
        this.roleConfigExpr = Objects.requireNonNull(roleConfigExpr);
        this.configValueRecorder = Objects.requireNonNull(configValueRecorder);
    }

    public String getRoleConfigExpr() {
        return roleConfigExpr;
    }

    public BiConsumer<String, Supplier<String[]>> getConfigValueRecorder() {
        return configValueRecorder;
    }

    public static boolean isSecurityConfigExpressionCandidate(String configExpression) {
        if (configExpression == null || configExpression.length() < 4) {
            return false;
        }
        final int exprStart = configExpression.indexOf("${");
        return exprStart >= 0 && configExpression.indexOf('}', exprStart + 2) > 0;
    }
}
