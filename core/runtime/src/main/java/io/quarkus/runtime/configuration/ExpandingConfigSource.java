package io.quarkus.runtime.configuration;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.wildfly.common.Assert;
import org.wildfly.common.expression.Expression;

/**
 * A value-expanding configuration source, which allows (limited) recursive expansion.
 */
public class ExpandingConfigSource extends AbstractDelegatingConfigSource {

    private static final long serialVersionUID = 1075000015425893741L;
    private static final ThreadLocal<Boolean> NO_EXPAND = new ThreadLocal<>();

    public static UnaryOperator<ConfigSource> wrapper(Cache cache) {
        return configSource -> new ExpandingConfigSource(configSource, cache);
    }

    private final Cache cache;

    /**
     * Construct a new instance.
     *
     * @param delegate the delegate config source (must not be {@code null})
     * @param cache the cache instance to use (must not be {@code null})
     */
    public ExpandingConfigSource(final ConfigSource delegate, final Cache cache) {
        super(delegate);
        Assert.checkNotNullParam("cache", cache);
        this.cache = cache;
    }

    @Override
    public Set<String> getPropertyNames() {
        return delegate.getPropertyNames();
    }

    @Override
    public String getValue(final String propertyName) {
        final String delegateValue = delegate.getValue(propertyName);
        return isExpanding() ? expand(delegateValue) : delegateValue;
    }

    Object writeReplace() throws ObjectStreamException {
        return new Ser(delegate);
    }

    static final class Ser implements Serializable {
        private static final long serialVersionUID = 3633535720479375279L;

        final ConfigSource d;

        Ser(final ConfigSource d) {
            this.d = d;
        }

        Object readResolve() {
            return new ExpandingConfigSource(d, new Cache());
        }
    }

    String expand(final String value) {
        return expandValue(value, cache);
    }

    public void flush() {
        cache.flush();
    }

    public String toString() {
        return "ExpandingConfigSource[delegate=" + getDelegate() + ",ord=" + getOrdinal() + "]";
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

    public static String expandValue(String value, Cache cache) {
        if (value == null)
            return null;
        final Expression compiled = cache.exprCache.computeIfAbsent(value,
                str -> Expression.compile(str, Expression.Flag.LENIENT_SYNTAX, Expression.Flag.NO_TRIM));
        return compiled.evaluate(ConfigExpander.INSTANCE);
    }

    /**
     * An expression cache to use with {@link ExpandingConfigSource}.
     */
    public static final class Cache implements Serializable {
        private static final long serialVersionUID = 6111143168103886992L;

        // this is a cache of compiled expressions, NOT a cache of expanded values
        final ConcurrentHashMap<String, Expression> exprCache = new ConcurrentHashMap<>();

        public Cache() {
        }

        public void flush() {
            exprCache.clear();
        }
    }
}
