package io.quarkus.kafka.client.runtime.graal;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

import org.apache.kafka.common.utils.ByteBufferUnmapper;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.jdk.JDK8OrEarlier;

@TargetClass(value = ByteBufferUnmapper.class, onlyWith = JDK8OrEarlier.class)
final class Target_org_apache_kafka_common_utils_ByteBufferUnmapper {

    @Substitute
    public static void unmap(String resourceDescription, ByteBuffer buffer) throws IOException {
        if (!buffer.isDirect())
            throw new IllegalArgumentException("Unmapping only works with direct buffers");
        try {
            Method cleanerMethod = buffer.getClass().getMethod("cleaner");
            cleanerMethod.setAccessible(true);
            Object cleaner = cleanerMethod.invoke(buffer);
            Method cleanMethod = cleaner.getClass().getMethod("clean");
            cleanMethod.setAccessible(true);
            cleanMethod.invoke(cleaner);
        } catch (Throwable t) {
            throw new IOException("Unable to unmap the mapped buffer: " + resourceDescription, t);
        }

    }
}
