package io.quarkus.infinispan.embedded.runtime.graal;

import org.infinispan.Version;
import org.infinispan.commons.jdkspecific.CallerId;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Both variations using a security manager or using Reflection class require loading up JNI
 */
@TargetClass(CallerId.class)
@Substitute
public final class SubstituteCallerId {

    @Substitute
    public static Class<?> getCallerClass(int n) {
        // Can't figure out how to make sure security is working properly - so just passing back a class that is known to be good
        return Version.class;
    }
}
