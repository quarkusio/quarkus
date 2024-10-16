package io.quarkus.kafka.client.runtime.graal;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import sun.misc.Unsafe;

@TargetClass(className = "org.apache.kafka.common.network.SaslChannelBuilder")
final class Target_org_apache_kafka_common_network_SaslChannelBuilder {

    @Substitute

    private static String defaultKerberosRealm() throws ClassNotFoundException, NoSuchMethodException,
            IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        throw new RuntimeException("Not implemented on native");
    }

}

@TargetClass(className = "org.apache.kafka.shaded.com.google.protobuf.UnsafeUtil")
final class Target_org_apache_kafka_shaded_com_google_protobuf_UnsafeUtil {
    @Substitute
    static sun.misc.Unsafe getUnsafe() {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            return (Unsafe) theUnsafe.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

class KafkaSubstitutions {

}
