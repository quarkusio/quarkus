package io.quarkus.test.junit.internal;

import java.util.function.Supplier;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConversionException;

/**
 * Super simple cloning strategy that just serializes to XML and deserializes it using xstream
 */
class XStreamDeepClone implements DeepClone {

    static final String DEFAULT_ACTION = "Please report the issue on the Quarkus issue tracker.";

    private final Supplier<XStream> xStreamSupplier;

    XStreamDeepClone(ClassLoader classLoader) {
        // avoid doing any work eagerly since the cloner is rarely used
        xStreamSupplier = () -> {
            XStream result = new XStream();
            XStream.setupDefaultSecurity(result);
            result.allowTypesByRegExp(new String[] { ".*" });
            result.setClassLoader(classLoader);
            return result;
        };
    }

    @Override
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
        XStream xStream = xStreamSupplier.get();
        final String serialized = serialize(objectToClone, xStream);
        final Object result = xStream.fromXML(serialized);
        if (result == null) {
            throw new IllegalStateException(cannotDeepCloneMessage(objectToClone, DEFAULT_ACTION));
        }
        return result;
    }

    private String serialize(Object objectToClone, XStream xStream) {
        try {
            return xStream.toXML(objectToClone);
        } catch (ConversionException e) {
            String additional = e.getMessage().startsWith("No converter available")
                    ? "'No converter available' messages are known to occur with new-ish Java versions due " +
                            "to https://github.com/x-stream/xstream/issues/253, please try with JVM options proposed in " +
                            "the message of the nested ConversionException, " +
                            "if any, or report the issue on the Quarkus issue tracker.\n" + e.getMessage()
                    : DEFAULT_ACTION;
            throw new IllegalStateException(cannotDeepCloneMessage(objectToClone, additional), e);
        }
    }

    private String cannotDeepCloneMessage(Object objectToClone, String additional) {
        return String.format("Unable to deep clone object of type '%s'. %s", objectToClone.getClass().getName(), additional);
    }
}
