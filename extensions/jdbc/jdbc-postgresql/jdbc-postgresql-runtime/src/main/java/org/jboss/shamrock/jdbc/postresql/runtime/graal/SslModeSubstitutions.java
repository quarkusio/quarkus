package org.jboss.shamrock.jdbc.postresql.runtime.graal;

import java.util.Properties;

import org.postgresql.jdbc.SslMode;
import org.postgresql.util.PSQLException;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * SSL is not supported by graalVM yet
 */
@TargetClass(SslMode.class)
public final class SslModeSubstitutions {
    @Substitute
    public static SslMode of(Properties info) throws PSQLException {
        return SslMode.DISABLE;
    }
}
