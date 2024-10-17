package io.quarkus.jdbc.singlestore.runtime.graal;

import java.io.IOException;
import java.net.Socket;
import java.sql.SQLException;

import com.singlestore.jdbc.Configuration;
import com.singlestore.jdbc.HostAddress;
import com.singlestore.jdbc.client.impl.ConnectionHelper;
import com.singlestore.jdbc.client.socket.impl.SocketHandlerFunction;

public class SimpleSocketHandlerFunction implements SocketHandlerFunction {
    @Override
    public Socket apply(Configuration conf, HostAddress hostAddress) throws IOException, SQLException {
        if (conf.pipe() != null) {
            throw new IllegalArgumentException(getErrorMessage("pipe"));
        } else if (conf.localSocket() != null) {
            throw new IllegalArgumentException(getErrorMessage("localSocket"));
        }

        return ConnectionHelper.standardSocket(conf, hostAddress);
    }

    private String getErrorMessage(String option) {
        return "Option '" + option + "' is not available for Singlestore in native mode";
    }
}
