package io.quarkus.bootstrap.logging;

import java.util.Collections;
import java.util.Map;
import org.jboss.logmanager.MDCProvider;

/**
 * Class enabling Quarkus to instantiate a {@link MDCProvider}
 * and set a delegate during runtime initialization.
 *
 * While no delegate is set it serves as a NOP MDC.
 *
 * LateBoundMDCProvider is an implementation of the MDC Provider SPI
 * it will only be used/discovered if a provider configuration file
 * {@code META-INF/services/org.jboss.logmanager.MDCProvider } is created.
 */
@SuppressWarnings({ "unused" })
public class LateBoundMDCProvider implements MDCProvider {
    private static volatile MDCProvider delegate;

    /**
     * Set the actual {@link MDCProvider} to use as the delegate.
     *
     * @param delegate Properly constructed {@link MDCProvider}.
     */
    public static void setMDCProviderDelegate(MDCProvider delegate) {
        LateBoundMDCProvider.delegate = delegate;
    }

    @Override
    public String get(String key) {
        if (delegate == null) {
            return null;
        }
        return delegate.get(key);
    }

    @Override
    public Object getObject(String key) {
        if (delegate == null) {
            return null;
        }
        return delegate.getObject(key);
    }

    @Override
    public String put(String key, String value) {
        if (delegate == null) {
            return null;
        }
        return delegate.put(key, value);
    }

    @Override
    public Object putObject(String key, Object value) {
        if (delegate == null) {
            return null;
        }
        return delegate.putObject(key, value);
    }

    @Override
    public String remove(String key) {
        if (delegate == null) {
            return null;
        }
        return delegate.remove(key);
    }

    @Override
    public Object removeObject(String key) {
        if (delegate == null) {
            return null;
        }
        return delegate.removeObject(key);
    }

    @Override
    public Map<String, String> copy() {
        if (delegate == null) {
            return Collections.emptyMap();
        }
        return delegate.copy();
    }

    @Override
    public Map<String, Object> copyObject() {
        if (delegate == null) {
            return Collections.emptyMap();
        }
        return delegate.copyObject();
    }

    @Override
    public void clear() {
        if (delegate == null) {
            return;
        }
        delegate.clear();
    }
}
