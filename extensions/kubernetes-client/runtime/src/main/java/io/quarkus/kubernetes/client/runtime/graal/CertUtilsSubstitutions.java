package io.quarkus.kubernetes.client.runtime.graal;

import java.io.IOException;
import java.io.InputStream;
import java.security.PrivateKey;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "io.fabric8.kubernetes.client.internal.CertUtils")
public final class CertUtilsSubstitutions {

    // take out this method so we can run the native image without '--allow-incomplete-classpath'
    @Substitute
    static PrivateKey handleECKey(InputStream keyInputStream) throws IOException {
        throw new RuntimeException("EC Keys are not supported when using the native binary");
    }
}
