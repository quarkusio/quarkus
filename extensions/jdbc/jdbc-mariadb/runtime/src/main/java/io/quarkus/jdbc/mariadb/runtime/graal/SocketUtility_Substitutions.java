package io.quarkus.jdbc.mariadb.runtime.graal;

import org.mariadb.jdbc.client.socket.impl.SocketHandlerFunction;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(org.mariadb.jdbc.client.socket.impl.SocketUtility.class)
public final class SocketUtility_Substitutions {

    // Ensure that JNA is never used
    @Substitute
    public static SocketHandlerFunction getSocketHandler() {
        return new SimpleSocketHandlerFunction();
    }
}
