package io.quarkus.jackson.runtime.graal;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.oracle.svm.core.annotate.Alias;
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
 * Substitutes {@link OptionalHandlerFactory} so the DOM handlers ({@code DOMSerializer},
 * {@code DOMDeserializer}) are loaded reflectively instead of being instantiated directly.
 * <p>
 * Jackson 2 loaded these handlers through a string-based {@code Class.forName(...)}, which kept
 * them — and the JDK XML transformer/parser implementations their initialization drags in — out
 * of native images unless explicitly registered. Jackson 3 instantiates them directly, making
 * {@code javax.xml.transform.TransformerFactory} and its Xalan implementation statically
 * reachable in every native image that contains Jackson. This substitution restores the
 * Jackson 2 behavior for native images: applications that do not serialize DOM types do not pay
 * for the XML parsers, while applications that do can opt in by registering the DOM handlers for
 * reflection.
 * <p>
 * See <a href="https://github.com/quarkusio/quarkus/issues/55650">GitHub issue #55650</a>.
 */
@TargetClass(OptionalHandlerFactory.class)
final class Target_OptionalHandlerFactory {

    @Substitute
    public ValueSerializer<?> findSerializer(SerializationConfig config, JavaType type) {
        final Class<?> rawType = type.getRawClass();
        if (Node.class.isAssignableFrom(rawType)) {
            return (ValueSerializer<?>) JacksonDomHandlers.instantiate(JacksonDomHandlers.domSerializer);
        }

        String className = rawType.getName();
        if (className.startsWith(JacksonDomHandlers.PACKAGE_PREFIX_JAVAX_XML)
                || JacksonDomHandlers.hasSuperClassStartingWith(rawType, JacksonDomHandlers.PACKAGE_PREFIX_JAVAX_XML)) {
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
        if (Node.class.isAssignableFrom(rawType)) {
            return (ValueDeserializer<?>) JacksonDomHandlers.instantiate(JacksonDomHandlers.nodeDeserializer);
        }
        if (Document.class.isAssignableFrom(rawType)) {
            return (ValueDeserializer<?>) JacksonDomHandlers.instantiate(JacksonDomHandlers.documentDeserializer);
        }
        String className = rawType.getName();
        if (className.startsWith(JacksonDomHandlers.PACKAGE_PREFIX_JAVAX_XML)
                || JacksonDomHandlers.hasSuperClassStartingWith(rawType, JacksonDomHandlers.PACKAGE_PREFIX_JAVAX_XML)) {
            return CoreXMLDeserializers.findBeanDeserializer(config, type);
        }
        return JavaSqlTypeHandlerFactory.instance.findDeserializer(config, type);
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

final class JacksonDomHandlers {

    static final String PACKAGE_PREFIX_JAVAX_XML = "javax.xml.";

    // These fields are deliberately NOT final: a constant class name could be folded into the
    // Class.forName(...) call during native image analysis (e.g. through inlining before
    // analysis), which would make the DOM handlers statically reachable again and defeat the
    // purpose of this indirection.
    static String domSerializer = "tools.jackson.databind.ext.DOMSerializer";
    static String nodeDeserializer = "tools.jackson.databind.ext.DOMDeserializer$NodeDeserializer";
    static String documentDeserializer = "tools.jackson.databind.ext.DOMDeserializer$DocumentDeserializer";

    private JacksonDomHandlers() {
    }

    static Object instantiate(String className) {
        try {
            return Class.forName(className).getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot instantiate `" + className
                    + "`: support for serializing DOM types is excluded from native images by default to keep"
                    + " the JDK XML parsers out of the image. To enable it, register the class for reflection,"
                    + " for example with `@RegisterForReflection(targets = ...)`.", e);
        }
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
