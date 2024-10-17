package io.quarkus.jdbc.singlestore.runtime.graal;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.TargetClass;
import com.singlestore.jdbc.client.socket.impl.SocketHandlerFunction;

@TargetClass(className = "com.singlestore.jdbc.client.impl.ConnectionHelper")
public final class ConnectionHelper_socketHandler {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static SocketHandlerFunction socketHandler = new SimpleSocketHandlerFunction();
}
