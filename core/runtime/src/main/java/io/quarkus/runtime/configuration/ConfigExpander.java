package io.quarkus.runtime.configuration;

import java.util.Optional;
import java.util.function.BiConsumer;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.wildfly.common.expression.Expression;
import org.wildfly.common.expression.ResolveContext;

/**
 * A property value expander that works with {@link Config}. This can be passed in as an expander
 * to {@link Expression#evaluate(BiConsumer)}.
 */
public final class ConfigExpander implements BiConsumer<ResolveContext<RuntimeException>, StringBuilder> {

    public static final ConfigExpander INSTANCE = new ConfigExpander();

    static final int MAX_DEPTH = 32;
    // substitute
    private static final ThreadLocal<int[]> depth = ThreadLocal.withInitial(() -> new int[1]);

    private ConfigExpander() {
    }

    // substitute
    private static boolean enter() {
        final int[] depthArray = depth.get();
        if (depthArray[0] == MAX_DEPTH) {
            return false;
        }
        depthArray[0]++;
        return true;
    }

    // substitute
    private static void exit() {
        depth.get()[0]--;
    }

    public void accept(final ResolveContext<RuntimeException> context, final StringBuilder stringBuilder) {
        if (!enter()) {
            throw new IllegalArgumentException("Nested recursive expansion is too deep");
        } else
            try {
                final String key = context.getKey();
                final Optional<String> expanded = ConfigProvider.getConfig().getOptionalValue(key, String.class);
                if (expanded.isPresent()) {
                    stringBuilder.append(expanded.get());
                } else {
                    context.expandDefault();
                }
            } finally {
                exit();
            }
    }
}
