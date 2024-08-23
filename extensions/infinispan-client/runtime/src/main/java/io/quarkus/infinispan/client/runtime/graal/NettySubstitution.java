package io.quarkus.infinispan.client.runtime.graal;

import org.infinispan.client.hotrod.impl.transport.netty.NativeTransport;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

final class NettySubstitution {
}

@TargetClass(value = NativeTransport.class)
final class SubstituteNativeTransport {
    @Substitute
    private static boolean useNativeEpoll() {
        return false;
    }
}
