package io.quarkus.jdbc.mariadb.runtime.graal;

import org.mariadb.jdbc.Configuration;
import org.mariadb.jdbc.HostAddress;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.mariadb.jdbc.plugin.authentication.standard.SendPamAuthPacketFactory")
public final class SendPamAuthPacketFactory_Substitutions {

    @Substitute
    public void initialize(String authenticationData, byte[] seed, Configuration conf, HostAddress hostAddress) {
        throw new UnsupportedOperationException("Authentication strategy 'dialog' is not supported in GraalVM");
    }

}
