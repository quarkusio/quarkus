package io.quarkus.jdbc.mariadb.runtime.graal;

import org.mariadb.jdbc.client.socket.impl.SocketHandlerFunction;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.mariadb.jdbc.client.impl.ConnectionHelper")
public final class ConnectionHelper_socketHandler {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static SocketHandlerFunction socketHandler = new SimpleSocketHandlerFunction();
}
