package io.quarkus.confluent.registry.common.graal;

import java.net.Socket;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * GraalVM substitution for Confluent Schema Registry client's SSL socket factory.
 *
 * {@code HostSslSocketFactory.interceptAndSetHost()} references
 * {@code org.bouncycastle.jsse.BCSSLSocket} for FIPS deployment support.
 * BouncyCastle JSSE is an optional dependency not on the classpath, causing
 * GraalVM native image to fail during linking.
 *
 * This substitution removes the BouncyCastle reference by returning the socket as-is,
 * which is the same behavior as non-FIPS deployments. BouncyCastle FIPS support
 * is not available in native mode.
 */
@TargetClass(className = "io.confluent.kafka.schemaregistry.client.ssl.HostSslSocketFactory")
final class Target_HostSslSocketFactory {

    @Substitute
    private Socket interceptAndSetHost(Socket socket) {
        return socket;
    }
}

class ConfluentRegistrySubstitutions {
}
