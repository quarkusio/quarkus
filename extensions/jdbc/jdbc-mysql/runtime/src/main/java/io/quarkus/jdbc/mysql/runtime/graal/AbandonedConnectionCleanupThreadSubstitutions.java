package io.quarkus.jdbc.mysql.runtime.graal;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.mysql.cj.MysqlConnection;
import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;
import com.mysql.cj.protocol.NetworkResources;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * This is to disable connection clean up thread {@link AbandonedConnectionCleanupThread} which launches a thread on a static
 * block.
 * GraalVM is not happy about that. The issue might have been fixed with https://github.com/oracle/graal/pull/1542 but we have
 * to wait for a proper GraalVM release, so we substitute the class and start the clean up thread manually when running in
 * native image.
 */
@Substitute
@TargetClass(AbandonedConnectionCleanupThread.class)
final public class AbandonedConnectionCleanupThreadSubstitutions implements Runnable {

    private static Set<ConnectionFinalizerPhantomReference> connectionFinalizerPhantomReferences;
    private static ReferenceQueue<MysqlConnection> mysqlConnectionReferenceQueue;

    private static ExecutorService executorService;

    @Substitute
    private AbandonedConnectionCleanupThreadSubstitutions() {
    }

    public static void startCleanUp() {
        connectionFinalizerPhantomReferences = ConcurrentHashMap.newKeySet();
        mysqlConnectionReferenceQueue = new ReferenceQueue<>();
        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.execute(new AbandonedConnectionCleanupThreadSubstitutions());
    }

    @Substitute
    public void run() {
        for (;;) {
            try {
                Reference<? extends MysqlConnection> reference = mysqlConnectionReferenceQueue.remove(5000);
                if (reference != null) {
                    finalizeResourceAndRemoveReference((ConnectionFinalizerPhantomReference) reference);
                }
            } catch (InterruptedException e) {
                synchronized (mysqlConnectionReferenceQueue) {
                    Reference<? extends MysqlConnection> reference;
                    while ((reference = mysqlConnectionReferenceQueue.poll()) != null) {
                        finalizeResourceAndRemoveReference((ConnectionFinalizerPhantomReference) reference);
                    }
                    connectionFinalizerPhantomReferences.clear();
                }

                return;
            } catch (Exception ex) {
            }
        }
    }

    @Substitute
    protected static void trackConnection(MysqlConnection conn, NetworkResources io) {
        synchronized (mysqlConnectionReferenceQueue) {
            ConnectionFinalizerPhantomReference reference = new ConnectionFinalizerPhantomReference(conn, io,
                    mysqlConnectionReferenceQueue);
            connectionFinalizerPhantomReferences.add(reference);
        }
    }

    @Substitute
    public static void uncheckedShutdown() {
        executorService.shutdownNow();
    }

    private static void finalizeResourceAndRemoveReference(ConnectionFinalizerPhantomReference reference) {
        try {
            reference.finalizeResources();
            reference.clear();
        } finally {
            connectionFinalizerPhantomReferences.remove(reference);
        }
    }

    private static class ConnectionFinalizerPhantomReference extends PhantomReference<MysqlConnection> {
        private NetworkResources networkResources;

        ConnectionFinalizerPhantomReference(MysqlConnection conn, NetworkResources networkResources,
                ReferenceQueue<? super MysqlConnection> refQueue) {
            super(conn, refQueue);
            this.networkResources = networkResources;
        }

        void finalizeResources() {
            if (this.networkResources != null) {
                try {
                    this.networkResources.forceClose();
                } finally {
                    this.networkResources = null;
                }
            }
        }
    }
}
