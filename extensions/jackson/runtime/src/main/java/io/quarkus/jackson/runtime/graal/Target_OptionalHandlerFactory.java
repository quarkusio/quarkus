package io.quarkus.jackson.runtime.graal;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.ext.CoreXMLDeserializers;
import tools.jackson.databind.ext.OptionalHandlerFactory;
import tools.jackson.databind.ext.QNameSerializer;
import tools.jackson.databind.ext.XMLGregorianCalendarSerializer;
import tools.jackson.databind.ext.sql.JavaSqlTypeHandlerFactory;
import tools.jackson.databind.ser.std.ToStringSerializer;

/**
 * Substitutes {@link OptionalHandlerFactory} so that the DOM handlers ({@code DOMSerializer},
 * {@code DOMDeserializer}) are only part of a native image when the application can actually use them.
 * <p>
 * Jackson 2 loaded these handlers through a string-based {@code Class.forName(...)}, which kept
 * them — and the JDK XML parser and transformer implementations their initialization drags in —
 * out of native images unless explicitly registered. Jackson 3 instantiates them directly and holds
 * {@code Node.class} and {@code Document.class} constants, making {@code javax.xml.transform.TransformerFactory}
 * and its Xalan implementation statically reachable in every native image that contains Jackson.
 * <p>
 * This substitution detects the DOM types by name and loads the handlers reflectively, as Jackson 2
 * did, and {@link JacksonSerializerRegistrationFeature} registers the handlers for reflection only
 * when a DOM type is reachable in the image. In an image where no DOM type is reachable, DOM
 * (de)serialization fails with a message explaining how to register the handlers explicitly.
 * <p>
 * See <a href="https://github.com/quarkusio/quarkus/issues/55650">GitHub issue #55650</a>.
 */
@TargetClass(OptionalHandlerFactory.class)
final class Target_OptionalHandlerFactory {

    @Delete
    private static Class<?> CLASS_DOM_NODE;

    @Delete
    private static Class<?> CLASS_DOM_DOCUMENT;

    @Substitute
    public ValueSerializer<?> findSerializer(SerializationConfig config, JavaType type) {
        final Class<?> rawType = type.getRawClass();
        if (JacksonDomSupport.isNode(rawType)) {
            return JacksonDomSupport.newHandler(JacksonDomSupport.DOM_SERIALIZER, rawType);
        }

        String className = rawType.getName();
        if (className.startsWith(JacksonDomSupport.PACKAGE_PREFIX_JAVAX_XML)
                || JacksonDomSupport.hasSuperClassStartingWith(rawType, JacksonDomSupport.PACKAGE_PREFIX_JAVAX_XML)) {
            if (Duration.class.isAssignableFrom(rawType)) {
                return ToStringSerializer.instance;
            }
            if (QName.class.isAssignableFrom(rawType)) {
                return QNameSerializer.instance;
            }
            if (XMLGregorianCalendar.class.isAssignableFrom(rawType)) {
                return Target_XMLGregorianCalendarSerializer.instance;
            }
        }
        return JavaSqlTypeHandlerFactory.instance.findSerializer(config, type);
    }

    @Substitute
    public ValueDeserializer<?> findDeserializer(DeserializationConfig config, JavaType type) {
        final Class<?> rawType = type.getRawClass();
        if (JacksonDomSupport.isDocument(rawType)) {
            return JacksonDomSupport.newHandler(JacksonDomSupport.DOM_DOCUMENT_DESERIALIZER, rawType);
        }
        if (JacksonDomSupport.isNode(rawType)) {
            return JacksonDomSupport.newHandler(JacksonDomSupport.DOM_NODE_DESERIALIZER, rawType);
        }
        String className = rawType.getName();
        if (className.startsWith(JacksonDomSupport.PACKAGE_PREFIX_JAVAX_XML)
                || JacksonDomSupport.hasSuperClassStartingWith(rawType, JacksonDomSupport.PACKAGE_PREFIX_JAVAX_XML)) {
            return CoreXMLDeserializers.findBeanDeserializer(config, type);
        }
        return JavaSqlTypeHandlerFactory.instance.findDeserializer(config, type);
    }

    @Substitute
    public boolean hasDeserializerFor(Class<?> valueType) {
        if (JacksonDomSupport.isNode(valueType)) {
            return true;
        }
        String className = valueType.getName();
        if (className.startsWith(JacksonDomSupport.PACKAGE_PREFIX_JAVAX_XML)
                || JacksonDomSupport.hasSuperClassStartingWith(valueType, JacksonDomSupport.PACKAGE_PREFIX_JAVAX_XML)) {
            return CoreXMLDeserializers.hasDeserializerFor(valueType);
        }
        return JavaSqlTypeHandlerFactory.instance.hasDeserializerFor(valueType);
    }
}

/**
 * Gives the substitution access to the package-private {@code instance} field.
 */
@TargetClass(XMLGregorianCalendarSerializer.class)
final class Target_XMLGregorianCalendarSerializer {

    @Alias
    static XMLGregorianCalendarSerializer instance;
}

final class JacksonDomSupport {

    static final String PACKAGE_PREFIX_JAVAX_XML = "javax.xml.";

    static final String DOM_SERIALIZER = "tools.jackson.databind.ext.DOMSerializer";
    static final String DOM_NODE_DESERIALIZER = "tools.jackson.databind.ext.DOMDeserializer$NodeDeserializer";
    static final String DOM_DOCUMENT_DESERIALIZER = "tools.jackson.databind.ext.DOMDeserializer$DocumentDeserializer";

    private static final String NODE = "org.w3c.dom.Node";
    private static final String DOCUMENT = "org.w3c.dom.Document";

    private JacksonDomSupport() {
    }

    static boolean isNode(Class<?> rawType) {
        return implementsByName(rawType, NODE);
    }

    static boolean isDocument(Class<?> rawType) {
        return implementsByName(rawType, DOCUMENT);
    }

    /**
     * Loads a DOM handler reflectively, so that the handler and the JDK XML implementation it initializes
     * are only linked into the image when {@link JacksonSerializerRegistrationFeature} registered it.
     */
    @SuppressWarnings("unchecked")
    static <T> T newHandler(String className, Class<?> rawType) {
        try {
            return (T) Class.forName(className).getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot serialize or deserialize `" + rawType.getName()
                    + "`: Jackson support for DOM types was not included in this native executable because no DOM type"
                    + " was found reachable when it was built. Register `" + className
                    + "` for reflection (for example with @RegisterForReflection(targets = ...)) to include it.", e);
        }
    }

    // by name, so that the org.w3c.dom types are not referenced by the substitution and only become reachable
    // when the application uses them
    private static boolean implementsByName(Class<?> rawType, String interfaceName) {
        for (Class<?> type = rawType; type != null && type != Object.class; type = type.getSuperclass()) {
            if (type.getName().equals(interfaceName)) {
                return true;
            }
            for (Class<?> iface : type.getInterfaces()) {
                if (implementsByName(iface, interfaceName)) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean hasSuperClassStartingWith(Class<?> rawType, String prefix) {
        for (Class<?> supertype = rawType.getSuperclass(); supertype != null; supertype = supertype.getSuperclass()) {
            if (supertype == Object.class) {
                return false;
            }
            if (supertype.getName().startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
