package io.quarkus.jdbc.mariadb.runtime.graal;

import org.mariadb.jdbc.internal.io.socket.SocketHandlerFunction;
import org.mariadb.jdbc.internal.util.Utils;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(org.mariadb.jdbc.internal.util.Utils.class)
public final class Utils_socketHandler {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static SocketHandlerFunction socketHandler = new SimpleSocketHandlerFunction();
}
