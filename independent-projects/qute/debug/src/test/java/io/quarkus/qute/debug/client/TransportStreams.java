package io.quarkus.qute.debug.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * This class is a copy of <a href=
 * "https://github.com/eclipse-lsp4e/lsp4e/blob/main/org.eclipse.lsp4e.debug/src/org/eclipse/lsp4e/debug/debugmodel/TransportStreams.java">TransportStreams.java</a>
 */
public abstract class TransportStreams {

    public InputStream in = null;
    public OutputStream out = null;

    public void close() {
        try {
            in.close();
        } catch (IOException e1) {
            // ignore inner resource exception
        }
        try {
            out.close();
        } catch (IOException e1) {
            // ignore inner resource exception
        }
    }

    public static class DefaultTransportStreams extends TransportStreams {
        public DefaultTransportStreams(InputStream in, OutputStream out) {
            this.in = in;
            this.out = out;
        }
    }

    public static class SocketTransportStreams extends TransportStreams {
        private final Socket socket;

        public SocketTransportStreams(String host, int port) {
            try {
                this.socket = new Socket(host, port);
                in = socket.getInputStream();
                out = socket.getOutputStream();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public void close() {
            super.close();
            try {
                socket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}