package io.quarkus.jdbc.postresql.runtime.graal;

import org.postgresql.core.PGStream;
import org.postgresql.core.v3.ConnectionFactoryImpl;
import org.postgresql.sspi.ISSPIClient;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(ConnectionFactoryImpl.class)
public final class DisableSSPIClient {

    @Substitute
    private ISSPIClient createSSPI(PGStream pgStream,
            String spnServiceClass,
            boolean enableNegotiate) {
        throw new IllegalStateException("The org.postgresql.sspi.SSPIClient is not available on GraalVM");
    }

}
