package io.quarkus.jdbc.h2.runtime.graal;

import org.h2.engine.ConnectionInfo;
import org.h2.engine.Session;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.h2.engine.Engine")
@Substitute
public final class Engine {

    @Substitute
    public static Session createSession(ConnectionInfo ci) {
        throw new UnsupportedOperationException(
                "H2 database compiled into a native-image is only functional as a client: can't create an Embedded Database Session");
    }

    @Substitute
    static void close(String name) {
        //no-op
    }

}
