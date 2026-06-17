package io.quarkus.jackson.runtime.graal;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

import com.fasterxml.jackson.databind.deser.std.DateDeserializers;
import com.fasterxml.jackson.databind.ext.CoreXMLDeserializers;
import com.fasterxml.jackson.databind.ext.CoreXMLSerializers;
import com.fasterxml.jackson.databind.ser.std.SqlDateSerializer;
import com.fasterxml.jackson.databind.ser.std.SqlTimeSerializer;

/**
 * Registers Jackson serializers/deserializers for reflection only when their corresponding
 * types are reachable during native image analysis. This avoids unconditionally pulling in
 * {@code java.sql.*} and {@code javax.xml.datatype.*} class hierarchies into the native image.
 *
 * See <a href="https://github.com/quarkusio/quarkus/issues/53818">GitHub issue #53818</a>.
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
                SqlDateSerializer.class,
                DateDeserializers.SqlDateDeserializer.class);

        registerWhenReachable(access, new Class<?>[] { Time.class }, true,
                SqlTimeSerializer.class);

        registerWhenReachable(access, new Class<?>[] { Timestamp.class }, true,
                DateDeserializers.TimestampDeserializer.class);

        // CoreXMLSerializers/CoreXMLDeserializers handle XMLGregorianCalendar, QName, and Duration
        registerWhenReachable(access, new Class<?>[] { XMLGregorianCalendar.class, QName.class, Duration.class }, false,
                CoreXMLSerializers.class,
                CoreXMLDeserializers.class);
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
