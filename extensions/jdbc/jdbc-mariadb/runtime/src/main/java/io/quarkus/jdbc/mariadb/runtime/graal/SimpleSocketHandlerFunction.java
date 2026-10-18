package io.quarkus.jdbc.mariadb.runtime.graal;

import java.io.IOException;
import java.net.Socket;
import java.sql.SQLException;

import org.mariadb.jdbc.Configuration;
import org.mariadb.jdbc.HostAddress;
import org.mariadb.jdbc.client.impl.ConnectionHelper;
import org.mariadb.jdbc.client.socket.impl.SocketHandlerFunction;

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
        return "Option '" + option + "' is not available for MariaDB in native mode";
    }
}
