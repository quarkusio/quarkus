package io.quarkus.infinispan.embedded.runtime.graal;

import java.security.SecureRandom;

import org.jgroups.util.UUID;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

public class SubstituteJGroups {
}

@TargetClass(UUID.class)
final class SubstituteUUID {
    @Alias
    // Force it to null - so it can be reinitialized
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
    protected static volatile SecureRandom numberGenerator;
}

// DISCARD protocol uses swing classes
@TargetClass(className = "org.jgroups.protocols.DISCARD")
final class SubstituteDiscardProtocol {

    @Substitute
    public void startGui() {
        // do nothing
    }

    @Substitute
    public void stopGui() {
        // do nothing
    }

    @Substitute
    public void start() throws Exception {
        // should call super.start() but the "super" Protocol.start() does nothing,
        // so this empty impl is OK
    }

    @Substitute
    public void stop() {
        // should call super.stop() but the "super" Protocol.stop() does nothing,
        // so this empty impl is OK

    }
}
