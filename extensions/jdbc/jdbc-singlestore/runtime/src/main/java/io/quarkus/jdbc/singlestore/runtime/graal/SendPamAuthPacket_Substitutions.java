package io.quarkus.jdbc.singlestore.runtime.graal;

import java.io.IOException;
import java.sql.SQLException;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.singlestore.jdbc.Configuration;
import com.singlestore.jdbc.client.Context;
import com.singlestore.jdbc.client.ReadableByteBuf;
import com.singlestore.jdbc.client.socket.Reader;
import com.singlestore.jdbc.client.socket.Writer;

@TargetClass(className = "com.singlestore.jdbc.plugin.authentication.standard.SendPamAuthPacket")
public final class SendPamAuthPacket_Substitutions {

    @Substitute
    public void initialize(String authenticationData, byte[] seed, Configuration conf) {
        throw new UnsupportedOperationException("Authentication strategy 'dialog' is not supported in GraalVM");
    }

    @Substitute
    public ReadableByteBuf process(Writer out, Reader in, Context context)
            throws SQLException, IOException {
        throw new UnsupportedOperationException("Authentication strategy 'dialog' is not supported in GraalVM");
    }
}
