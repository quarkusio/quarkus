package io.quarkus.jdbc.mariadb.runtime.graal;

import java.io.IOException;
import java.sql.SQLException;

import org.mariadb.jdbc.Configuration;
import org.mariadb.jdbc.HostAddress;
import org.mariadb.jdbc.client.Context;
import org.mariadb.jdbc.client.ReadableByteBuf;
import org.mariadb.jdbc.client.socket.Reader;
import org.mariadb.jdbc.client.socket.Writer;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.mariadb.jdbc.plugin.authentication.standard.SendPamAuthPacket")
public final class SendPamAuthPacket_Substitutions {

    @Substitute
    public void initialize(String authenticationData, byte[] seed, Configuration conf, HostAddress hostAddress) {
        throw new UnsupportedOperationException("Authentication strategy 'dialog' is not supported in GraalVM");
    }

    @Substitute
    public ReadableByteBuf process(Writer out, Reader in, Context context)
            throws SQLException, IOException {
        throw new UnsupportedOperationException("Authentication strategy 'dialog' is not supported in GraalVM");
    }
}
