package io.smallrye.reactive.kafka.graal;

import java.lang.reflect.InvocationTargetException;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.apache.kafka.common.network.SaslChannelBuilder")
final class Target_org_apache_kafka_common_network_SaslChannelBuilder {

    @Substitute

    private static String defaultKerberosRealm() throws ClassNotFoundException, NoSuchMethodException,
            IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        throw new RuntimeException("Not implemented on native");
    }

}

class KafkaSubstitutions {

}
