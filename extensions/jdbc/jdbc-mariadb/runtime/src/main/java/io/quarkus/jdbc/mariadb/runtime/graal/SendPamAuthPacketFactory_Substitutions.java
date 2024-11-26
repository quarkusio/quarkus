package io.quarkus.jdbc.mariadb.runtime.graal;

import org.mariadb.jdbc.Configuration;
import org.mariadb.jdbc.HostAddress;
import org.mariadb.jdbc.plugin.AuthenticationPlugin;
import org.mariadb.jdbc.plugin.authentication.standard.SendPamAuthPacketFactory;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * The SendPamAuthPacketFactory class is not supported in native mode.
 */
@TargetClass(SendPamAuthPacketFactory.class)
public final class SendPamAuthPacketFactory_Substitutions {

    @Substitute
    public AuthenticationPlugin initialize(String authenticationData, byte[] seed, Configuration conf,
            HostAddress hostAddress) {
        throw new UnsupportedOperationException("Authentication strategy 'dialog' is not supported in GraalVM");
    }

}
