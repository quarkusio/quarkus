package io.quarkus.jdbc.h2.runtime.graal;

import org.h2.engine.ConnectionInfo;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.AlwaysInline;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(ConnectionInfo.class)
public final class RemoteOnly {

    @Alias
    private boolean remote;

    @Substitute
    @AlwaysInline("Method org.h2.engine.SessionRemote.connectEmbeddedOrServer must be able to realize it's only ever going remote")
    public boolean isRemote() {
        if (this.remote == false) {
            throw new UnsupportedOperationException(
                    "H2 database compiled into a native-image is only functional as a client: can't create an Embedded Database Session");
        }
        return true;
    }

}
