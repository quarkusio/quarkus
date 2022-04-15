package io.quarkus.jdbc.h2.runtime.graal;

import org.h2.engine.ConnectionInfo;
import org.h2.engine.Session;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.h2.engine.SessionRemote")
public final class SessionRemote {

    @Alias
    private ConnectionInfo connectionInfo;

    /**
     * Even if in SessionRemote, this method originally can instantiate a local engine.
     * We don't want that as we don't support a local engine.
     */
    @Substitute
    public Session connectEmbeddedOrServer(boolean openNew) {
        ConnectionInfo ci = connectionInfo;
        if (ci.isRemote()) {
            connectServer(ci);
            return (Session) (Object) this;
        }

        throw new UnsupportedOperationException(
                "H2 database compiled into a native-image is only functional as a client: can't create an Embedded Database Session");
    }

    @Alias
    private void connectServer(ConnectionInfo ci) {
    }
}
