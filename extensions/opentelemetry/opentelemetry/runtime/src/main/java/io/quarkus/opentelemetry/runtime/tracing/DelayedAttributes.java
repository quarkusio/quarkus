package io.quarkus.opentelemetry.runtime.tracing;

import java.util.Collections;
import java.util.Map;
import java.util.function.BiConsumer;

import org.jboss.logging.Logger;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;

/**
 * Class enabling Quarkus to instantiate a {@link io.opentelemetry.api.trace.TracerProvider}
 * during static initialization and set a {@link Attributes} delegate during runtime initialization.
 */
public class DelayedAttributes implements Attributes {
    private static final Logger log = Logger.getLogger(DelayedAttributes.class);
    private boolean warningLogged = false;

    private Attributes delegate;

    /**
     * Set the actual {@link Attributes} to use as the delegate.
     *
     * @param delegate Properly constructed {@link Attributes}.
     */
    public void setAttributesDelegate(Attributes delegate) {
        this.delegate = delegate;
    }

    @Override
    public <T> T get(AttributeKey<T> attributeKey) {
        if (delegate == null) {
            logDelegateNotFound();
            return null;
        }
        return delegate.get(attributeKey);
    }

    @Override
    public void forEach(BiConsumer<? super AttributeKey<?>, ? super Object> biConsumer) {
        if (delegate == null) {
            logDelegateNotFound();
            return;
        }
        delegate.forEach(biConsumer);
    }

    @Override
    public int size() {
        if (delegate == null) {
            logDelegateNotFound();
            return 0;
        }
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        if (delegate == null) {
            logDelegateNotFound();
            return true;
        }
        return delegate.isEmpty();
    }

    @Override
    public Map<AttributeKey<?>, Object> asMap() {
        if (delegate == null) {
            logDelegateNotFound();
            return Collections.emptyMap();
        }
        return delegate.asMap();
    }

    @Override
    public AttributesBuilder toBuilder() {
        if (delegate == null) {
            logDelegateNotFound();
            return Attributes.builder();
        }
        return delegate.toBuilder();
    }

    /**
     * If we haven't previously logged an error,
     * log an error about a missing {@code delegate} and set {@code warningLogged=true}
     */
    private void logDelegateNotFound() {
        if (!warningLogged) {
            log.warn("No Attributes delegate specified, no action taken.");
            warningLogged = true;
        }
    }
}
