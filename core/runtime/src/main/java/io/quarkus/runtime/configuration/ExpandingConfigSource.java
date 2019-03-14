package io.quarkus.runtime.configuration;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.wildfly.common.expression.Expression;

import io.smallrye.config.SmallRyeConfigBuilder;

/**
 * A value-expanding configuration source, which allows (limited) recursive expansion.
 */
public class ExpandingConfigSource extends AbstractDelegatingConfigSource {
    // this is a cache of compiled expressions, NOT a cache of expanded values
    private final ConcurrentHashMap<String, Expression> exprCache = new ConcurrentHashMap<>();

    private static final ThreadLocal<Boolean> NO_EXPAND = new ThreadLocal<>();

    /**
     * A wrapper suitable for passing in to {@link SmallRyeConfigBuilder#withWrapper(UnaryOperator)}.
     */
    public static final UnaryOperator<ConfigSource> WRAPPER = ExpandingConfigSource::new;

    /**
     * Construct a new instance.
     *
     * @param delegate the delegate config source (must not be {@code null})
     */
    public ExpandingConfigSource(final ConfigSource delegate) {
        super(delegate);
    }

    public Set<String> getPropertyNames() {
        return delegate.getPropertyNames();
    }

    public String getValue(final String propertyName) {
        final String delegateValue = delegate.getValue(propertyName);
        return isExpanding() ? expand(delegateValue) : delegateValue;
    }

    String expand(final String value) {
        if (value == null)
            return null;
        final Expression compiled = exprCache.computeIfAbsent(value,
                str -> Expression.compile(str, Expression.Flag.ESCAPES, Expression.Flag.LENIENT_SYNTAX));
        return compiled.evaluate(ConfigExpander.INSTANCE);
    }

    public void flush() {
        exprCache.clear();
    }

    private static boolean isExpanding() {
        return NO_EXPAND.get() != Boolean.TRUE;
    }

    public static boolean setExpanding(boolean newValue) {
        try {
            return NO_EXPAND.get() != Boolean.TRUE;
        } finally {
            if (newValue) {
                NO_EXPAND.remove();
            } else {
                NO_EXPAND.set(Boolean.TRUE);
            }
        }
    }
}
