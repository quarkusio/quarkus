package io.quarkus.kafka.client.runtime.graal;

import java.lang.invoke.MethodHandle;
import java.util.zip.Checksum;

import org.apache.kafka.common.utils.Crc32C;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.RecomputeFieldValue.Kind;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.jdk.JDK11OrLater;

/**
 * The following substitution replaces the usage of {@code MethodHandle} in {@code Java9ChecksumFactory} with a plain
 * constructor invocation when run under GraalVM. This is necessary because the native image generator does not support method
 * handles.
 */
@TargetClass(value = Crc32C.class, innerClass = "Java9ChecksumFactory", onlyWith = JDK11OrLater.class)
final class Target_org_apache_kafka_common_utils_Crc32C_Java9ChecksumFactory {

    @Alias
    @RecomputeFieldValue(kind = Kind.Reset)
    private static MethodHandle CONSTRUCTOR;

    @Substitute
    public Checksum create() {
        try {
            return (Checksum) Class.forName("java.util.zip.CRC32C").getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
