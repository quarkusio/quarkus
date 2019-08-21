package io.quarkus.infinispan.embedded.runtime.graal;

import org.jgroups.Address;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * These substitutions need to be revisited on each new Quarkus/GraalVM release, as some methods may become
 * supported. The better option currently is to use -H:+ReportUnsupportedElementsAtRuntime, which moves potential issue
 * to run time
 *
 * @author Bela Ban
 * @since 1.0.0
 */
class JChannelSubstitutions {
}

@TargetClass(className = "org.jgroups.protocols.VERIFY_SUSPECT")
final class Target_org_jgroups_protocols_VERIFY_SUSPECT {

    @Substitute
    protected void verifySuspectWithICMP(Address suspected_mbr) {
        throw new UnsupportedOperationException("this method is currently unsupported");
    }

}
