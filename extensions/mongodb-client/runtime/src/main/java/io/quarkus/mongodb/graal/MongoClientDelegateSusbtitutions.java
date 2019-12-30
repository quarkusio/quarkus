package io.quarkus.mongodb.graal;

import com.mongodb.MongoInternalException;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.binding.ClusterBinding;
import com.mongodb.binding.ReadWriteBinding;
import com.mongodb.client.ClientSession;
import com.mongodb.client.internal.ClientSessionBinding;
import com.mongodb.client.internal.MongoClientDelegate;
import com.mongodb.connection.Cluster;
import com.mongodb.internal.binding.ClusterAwareReadWriteBinding;
import com.mongodb.internal.session.ServerSessionPool;
import com.mongodb.lang.Nullable;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(MongoClientDelegate.class)
public final class MongoClientDelegateSusbtitutions {
    @Alias
    private Cluster cluster;
    @Alias
    private ServerSessionPool serverSessionPool;

    @Substitute
    public void close() {
        serverSessionPool.close();
        cluster.close();
    }

    @TargetClass(className = "com.mongodb.client.internal.MongoClientDelegate$DelegateOperationExecutor")
    public final class DelegateOperationExecutorSubstitutions {
//        @Alias
//        private Cluster cluster;

        @Substitute
        ReadWriteBinding getReadWriteBinding(final ReadPreference readPreference, final ReadConcern readConcern,
                ClientSession session, final boolean ownsSession) {
            ClusterAwareReadWriteBinding readWriteBinding = new ClusterBinding(cluster,
                    getReadPreferenceForBinding(readPreference, session), readConcern);

            if (session != null) {
                return new ClientSessionBinding(session, ownsSession, readWriteBinding);
            } else {
                return readWriteBinding;
            }
        }

        @Substitute
        private ReadPreference getReadPreferenceForBinding(final ReadPreference readPreference,
                @Nullable final com.mongodb.client.ClientSession session) {
            if (session == null) {
                return readPreference;
            }
            if (session.hasActiveTransaction()) {
                ReadPreference readPreferenceForBinding = session.getTransactionOptions().getReadPreference();
                if (readPreferenceForBinding == null) {
                    throw new MongoInternalException(
                            "Invariant violated.  Transaction options read preference can not be null");
                }
                return readPreferenceForBinding;
            }
            return readPreference;
        }
    }
}
