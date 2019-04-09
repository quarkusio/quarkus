package io.quarkus.undertow.websockets.deployment;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.zip.InflaterInputStream;

import javax.websocket.HandshakeResponse;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.xnio.IoUtils;

import io.quarkus.deployment.devmode.HotReplacementContext;

@ServerEndpoint(value = HotReplacementWebsocketEndpoint.QUARKUS_HOT_RELOAD, configurator = HotReplacementWebsocketEndpoint.ServerConfigurator.class)
public class HotReplacementWebsocketEndpoint {

    static final String QUARKUS_HOT_RELOAD = "/quarkus/hot-reload";
    static final String QUARKUS_SECURITY_KEY = "quarkus-security-key";
    static final String QUARKUS_HOT_RELOAD_PASSWORD = "quarkus.hot-reload.password";
    private static Logger logger = Logger.getLogger(HotReplacementWebsocketEndpoint.class);

    private static final long MAX_WAIT_TIME = 15000;

    private static final int CLASS_CHANGE_RESPONSE = 2;
    private static final int CLASS_CHANGE_REQUEST = 1;

    private static final Object lock = new Object();
    /**
     * The current connection, managed under lock
     * <p>
     * There will only ever be one connection at a time
     */
    private static ConnectionContext connection;
    private ConnectionContext currentConnection;

    @OnClose
    void close() {
        synchronized (lock) {
            if (connection == currentConnection) {
                connection = null;
            }
        }
        this.currentConnection.messages.add(new Message()); //unblock a waiting thread
    }

    @OnError
    public void error(Throwable t) {
        logger.error("Error in hot replacement websocket connection", t);
        currentConnection.messages.add(new Message()); //unblock a waiting thread
        IoUtils.safeClose(currentConnection.connection);
    }

    @OnOpen
    public void onConnect(Session session) {
        synchronized (lock) {
            if (connection != null) {
                //only one open connection at a time
                IoUtils.safeClose(connection.connection);
                //add an empty message to unblock a waiting request
                connection.messages.add(new Message());
            }
            currentConnection = new ConnectionContext(session);
            this.connection = currentConnection;
        }
    }

    public static void checkForChanges(HotReplacementContext hrc) {
        final ConnectionContext con;
        synchronized (lock) {
            con = connection;
        }
        if (con == null) {
            //we return if there is no connection
            return;
        }
        try {
            con.connection.getBasicRemote().sendBinary(ByteBuffer.wrap(new byte[] { CLASS_CHANGE_REQUEST }));
        } catch (IOException e) {
            try {
                con.connection.close();
            } catch (IOException ignored) {

            }
            //add an empty message so the request can continue
            con.messages.add(new Message());
        }
        try {
            Message m = con.messages.poll(MAX_WAIT_TIME, TimeUnit.MILLISECONDS);
            if (m == null) {
                logger.error("Timed out processing hot replacement");
            } else {
                if (!m.srcFiles.isEmpty() ||
                        !m.resources.isEmpty()) {
                    if (hrc.getSourcesDir() != null) {
                        for (Map.Entry<String, byte[]> i : m.srcFiles.entrySet()) {
                            Path path = hrc.getSourcesDir().resolve(i.getKey());
                            Files.createDirectories(path.getParent());
                            try (FileOutputStream out = new FileOutputStream(
                                    path.toFile())) {
                                out.write(i.getValue());
                            }
                        }
                    }
                    if (hrc.getResourcesDir() != null) {
                        for (Map.Entry<String, byte[]> i : m.resources.entrySet()) {
                            Path path = hrc.getResourcesDir().resolve(i.getKey());
                            Files.createDirectories(path.getParent());
                            try (FileOutputStream out = new FileOutputStream(
                                    path.toFile())) {
                                out.write(i.getValue());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to process hot deployment", e);
        }
    }

    @OnMessage
    public void handleResponseMessage(byte[] message) throws IOException {
        byte first = message[0];
        if (first == CLASS_CHANGE_RESPONSE) {
            Message m = new Message();
            //a response message
            try (DataInputStream in = new DataInputStream(
                    new InflaterInputStream(new ByteArrayInputStream(message, 1, message.length - 1)))) {
                Map<String, byte[]> srcFiles = m.srcFiles;
                Map<String, byte[]> resources = m.resources;
                String key;
                byte[] rd;
                int count = in.readInt();
                for (int i = 0; i < count; ++i) {
                    key = in.readUTF();
                    int byteLength = in.readInt();
                    rd = new byte[byteLength];
                    in.readFully(rd);
                    srcFiles.put(key, rd);
                }
                count = in.readInt();
                for (int i = 0; i < count; ++i) {
                    key = in.readUTF();
                    int byteLength = in.readInt();
                    rd = new byte[byteLength];
                    in.readFully(rd);
                    resources.put(key, rd);
                }
                currentConnection.messages.add(m);
            }
        }
    }

    static final class Message {

        Map<String, byte[]> srcFiles = new HashMap<>();
        Map<String, byte[]> resources = new HashMap<>();
    }

    private static final class ConnectionContext {

        final Session connection;
        final BlockingDeque<Message> messages = new LinkedBlockingDeque<>();

        private ConnectionContext(Session connection) {
            this.connection = connection;
        }
    }

    public static final class ServerConfigurator extends ServerEndpointConfig.Configurator {
        @Override
        public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
            List<String> headers = request.getHeaders().get(QUARKUS_SECURITY_KEY);
            if (headers == null || headers.isEmpty()) {
                throw new RuntimeException("No security key present");
            }
            if (!headers.get(0).equals(WebsocketHotReloadSetup.replacementPassword)) {
                throw new RuntimeException("Security key did not match");
            }

            super.modifyHandshake(sec, request, response);
        }
    }
}
