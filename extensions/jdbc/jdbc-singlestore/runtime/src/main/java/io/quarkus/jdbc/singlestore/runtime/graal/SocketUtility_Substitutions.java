package io.quarkus.jdbc.singlestore.runtime.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.singlestore.jdbc.client.socket.impl.SocketHandlerFunction;

@TargetClass(com.singlestore.jdbc.client.socket.impl.SocketUtility.class)
public final class SocketUtility_Substitutions {

    // Ensure that JNA is never used
    @Substitute
    public static SocketHandlerFunction getSocketHandler() {
        return new SimpleSocketHandlerFunction();
    }
}
