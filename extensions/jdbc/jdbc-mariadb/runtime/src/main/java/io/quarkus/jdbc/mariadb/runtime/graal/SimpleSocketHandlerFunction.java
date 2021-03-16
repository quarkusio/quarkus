package io.quarkus.jdbc.mariadb.runtime.graal;

import java.io.IOException;
import java.net.Socket;

import org.mariadb.jdbc.internal.io.socket.SocketHandlerFunction;
import org.mariadb.jdbc.internal.util.Utils;
import org.mariadb.jdbc.util.Options;

public class SimpleSocketHandlerFunction implements SocketHandlerFunction {
    @Override
    public Socket apply(Options options, String host) throws IOException {
        if (options.pipe != null) {
            throw new IllegalArgumentException(getErrorMessage("pipe"));
        } else if (options.localSocket != null) {
            throw new IllegalArgumentException(getErrorMessage("localSocket"));
        } else if (options.sharedMemory != null) {
            throw new IllegalArgumentException(getErrorMessage("sharedMemory"));
        }

        return Utils.standardSocket(options, host);
    }

    private String getErrorMessage(String option) {
        return "Option '" + option + "' is not available for MariaDB in native mode";
    }
}
