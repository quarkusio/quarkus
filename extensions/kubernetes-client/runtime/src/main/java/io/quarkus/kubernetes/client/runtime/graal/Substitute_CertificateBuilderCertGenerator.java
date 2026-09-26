package io.quarkus.kubernetes.client.runtime.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "io.netty.handler.ssl.util.CertificateBuilderCertGenerator")
public final class Substitute_CertificateBuilderCertGenerator {

    @Substitute
    static boolean isAvailable() {
        return false;
    }

}
