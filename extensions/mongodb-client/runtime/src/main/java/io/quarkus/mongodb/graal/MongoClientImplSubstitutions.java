package io.quarkus.mongodb.graal;

import java.io.Closeable;
import java.io.IOException;

import com.mongodb.connection.Cluster;
import com.mongodb.diagnostics.logging.Logger;
import com.mongodb.internal.session.ServerSessionPool;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "com.mongodb.async.client.MongoClientImpl")
public final class MongoClientImplSubstitutions {
    @Alias
    private static Logger LOGGER;
    @Alias
    private Cluster cluster;
    @Alias
    private ServerSessionPool serverSessionPool;
    @Alias
    private Closeable externalResourceCloser;

    @Substitute
    public void close() {
        serverSessionPool.close();
        cluster.close();
        if (externalResourceCloser != null) {
            try {
                externalResourceCloser.close();
            } catch (IOException e) {
                LOGGER.warn("Exception closing resource", e);
            }
        }
    }
}
