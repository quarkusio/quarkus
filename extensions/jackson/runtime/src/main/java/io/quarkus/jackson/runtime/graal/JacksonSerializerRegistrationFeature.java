package io.quarkus.jackson.runtime.graal;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import tools.jackson.databind.ext.CoreXMLDeserializers;
import tools.jackson.databind.ext.DOMDeserializer;
import tools.jackson.databind.ext.DOMSerializer;
import tools.jackson.databind.ext.QNameSerializer;
import tools.jackson.databind.ext.XMLGregorianCalendarSerializer;
import tools.jackson.databind.ext.sql.JavaSqlDateDeserializer;
import tools.jackson.databind.ext.sql.JavaSqlDateSerializer;
import tools.jackson.databind.ext.sql.JavaSqlTimeSerializer;
import tools.jackson.databind.ext.sql.JavaSqlTimestampDeserializer;

/**
 * Registers Jackson serializers/deserializers for reflection only when their corresponding
 * types are reachable during native image analysis. This avoids unconditionally pulling in
 * {@code java.sql.*}, {@code javax.xml.datatype.*} and {@code org.w3c.dom.*} class hierarchies into the native image.
 *
 * See <a href="https://github.com/quarkusio/quarkus/issues/53818">GitHub issue #53818</a> and
 * <a href="https://github.com/quarkusio/quarkus/issues/55650">GitHub issue #55650</a>.
 */
public class JacksonSerializerRegistrationFeature implements Feature {

    @Override
    public String getDescription() {
        return "Conditionally registers Jackson SQL/XML serializers for reflection based on reachability";
    }

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        // java.sql serializers/deserializers
        registerWhenReachable(access, new Class<?>[] { Date.class }, true,
                JavaSqlDateSerializer.class,
                JavaSqlDateDeserializer.class);

        registerWhenReachable(access, new Class<?>[] { Time.class }, true,
                JavaSqlTimeSerializer.class);

        registerWhenReachable(access, new Class<?>[] { Timestamp.class }, true,
                JavaSqlTimestampDeserializer.class);

        // CoreXMLDeserializers handles XMLGregorianCalendar, QName, and Duration
        registerWhenReachable(access, new Class<?>[] { XMLGregorianCalendar.class, QName.class, Duration.class }, false,
                QNameSerializer.class,
                XMLGregorianCalendarSerializer.class,
                CoreXMLDeserializers.class);

        // DOM handlers, loaded reflectively by the OptionalHandlerFactory substitution: they initialize the JDK XML
        // parsers and transformers, which should only be in the image when a DOM type is reachable
        registerWhenReachable(access, new Class<?>[] { Node.class, Document.class }, false,
                DOMSerializer.class,
                DOMDeserializer.NodeDeserializer.class,
                DOMDeserializer.DocumentDeserializer.class);
    }

    private void registerWhenReachable(BeforeAnalysisAccess access, Class<?>[] triggerClasses,
            boolean includeMethods, Class<?>... classes) {
        for (Class<?> triggerClass : triggerClasses) {
            access.registerReachabilityHandler(a -> {
                for (Class<?> clazz : classes) {
                    RuntimeReflection.register(clazz);
                    RuntimeReflection.register(clazz.getDeclaredConstructors());
                    if (includeMethods) {
                        RuntimeReflection.register(clazz.getDeclaredMethods());
                    }
                }
            }, triggerClass);
        }
    }
}
