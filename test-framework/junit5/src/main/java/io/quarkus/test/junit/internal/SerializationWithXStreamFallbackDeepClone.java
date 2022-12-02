package io.quarkus.test.junit.internal;

import java.io.Serializable;
import java.util.Optional;

import org.jboss.logging.Logger;

/**
 * Cloning strategy delegating to {@link SerializationDeepClone}, falling back to {@link XStreamDeepClone} in case of error.
 */
public class SerializationWithXStreamFallbackDeepClone implements DeepClone {

    private static final Logger LOG = Logger.getLogger(SerializationWithXStreamFallbackDeepClone.class);

    private final SerializationDeepClone serializationDeepClone;
    private final XStreamDeepClone xStreamDeepClone;

    public SerializationWithXStreamFallbackDeepClone(ClassLoader classLoader) {
        this.serializationDeepClone = new SerializationDeepClone(classLoader);
        this.xStreamDeepClone = new XStreamDeepClone(classLoader);
    }

    @Override
    public Object clone(Object objectToClone) {
        if (objectToClone instanceof Serializable) {
            try {
                return serializationDeepClone.clone(objectToClone);
            } catch (RuntimeException re) {
                LOG.debugf("SerializationDeepClone failed (will fall back to XStream): %s",
                        Optional.ofNullable(re.getCause()).orElse(re));
            }
        }
        return xStreamDeepClone.clone(objectToClone);
    }
}
