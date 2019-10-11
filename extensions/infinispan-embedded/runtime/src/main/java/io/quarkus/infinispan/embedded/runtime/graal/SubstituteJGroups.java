package io.quarkus.infinispan.embedded.runtime.graal;

import java.security.SecureRandom;

import org.jgroups.util.UUID;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
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
