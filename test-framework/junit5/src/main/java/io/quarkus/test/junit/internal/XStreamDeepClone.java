package io.quarkus.test.junit.internal;

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
        final String serialized = xStream.toXML(objectToClone);
        return xStream.fromXML(serialized);
    }
}
