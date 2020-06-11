package io.quarkus.test.junit.internal;

import java.util.function.Supplier;

import com.thoughtworks.xstream.XStream;

/**
 * Super simple cloning strategy that just serializes to XML and deserializes it using xstream
 */
public class XStreamDeepClone implements DeepClone {

    private final XStream xStream;

    public XStreamDeepClone(ClassLoader classLoader) {
        xStream = new XStream();
        XStream.setupDefaultSecurity(xStream);
        xStream.allowTypesByRegExp(new String[] { ".*" });
        xStream.setClassLoader(classLoader);
    }

    public Object clone(Object objectToClone) {
        if (objectToClone == null) {
            return null;
        }

        if (objectToClone instanceof Supplier) {
            return handleSupplier((Supplier<?>) objectToClone);
        }

        return doClone(objectToClone);
    }

    private Supplier<Object> handleSupplier(final Supplier<?> supplier) {
        return new Supplier<Object>() {
            @Override
            public Object get() {
                return doClone(supplier.get());
            }
        };
    }

    private Object doClone(Object objectToClone) {
        final String serialized = xStream.toXML(objectToClone);
        final Object result = xStream.fromXML(serialized);
        if (result == null) {
            throw new IllegalStateException("Unable to deep clone object of type '" + objectToClone.getClass().getName()
                    + "'. Please report the issue on the Quarkus issue tracker.");
        }
        return result;
    }
}
