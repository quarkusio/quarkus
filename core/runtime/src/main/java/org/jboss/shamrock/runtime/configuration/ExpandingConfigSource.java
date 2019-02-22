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
        return expand(delegate.getValue(propertyName));
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
}
