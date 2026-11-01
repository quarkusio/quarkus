package io.quarkus.reactive.mysql.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.quarkus.test.QuarkusExtensionTest;
import io.vertx.sqlclient.Pool;

/**
 * Each datasource points to its own server that accepts TCP connections but never answers.
 * With {@code socket-read-idle-timeout}, the client closes the connection once the timeout expires;
 * without it, the connection stays open.
 */
public class SocketReadIdleTimeoutTest {

    static final SilentServer SERVER = SilentServer.shared("with-timeout");
    static final SilentServer SERVER_WITHOUT_TIMEOUT = SilentServer.shared("without-timeout");

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.datasource.db-kind", "mysql")
            .overrideConfigKey("quarkus.datasource.reactive.url", SERVER.url("mysql"))
            .overrideConfigKey("quarkus.datasource.reactive.socket-read-idle-timeout", "500ms")
            .overrideConfigKey("quarkus.datasource.\"no-idle-timeout\".db-kind", "mysql")
            .overrideConfigKey("quarkus.datasource.\"no-idle-timeout\".reactive.url",
                    SERVER_WITHOUT_TIMEOUT.url("mysql"));

    @Inject
    Pool pool;

    @Inject
    @ReactiveDataSource("no-idle-timeout")
    Pool poolWithoutIdleTimeout;

    @AfterAll
    static void stopServers() {
        SERVER.close();
        SERVER_WITHOUT_TIMEOUT.close();
    }

    @Test
    public void clientClosesConnectionWhenServerStaysSilent() throws InterruptedException {
        pool.query("SELECT 1").execute();

        assertThat(SERVER.accepted.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(SERVER.closedByClient.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void withoutIdleTimeoutConnectionStaysOpen() throws InterruptedException {
        poolWithoutIdleTimeout.query("SELECT 1").execute();

        assertThat(SERVER_WITHOUT_TIMEOUT.accepted.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(SERVER_WITHOUT_TIMEOUT.closedByClient.await(3, TimeUnit.SECONDS)).isFalse();
    }

    static final class SilentServer implements AutoCloseable {

        final CountDownLatch accepted;
        final CountDownLatch closedByClient;

        private final String key;
        private final ServerSocket serverSocket;
        private final List<Socket> sockets;

        @SuppressWarnings("unchecked")
        private SilentServer(String key, Object[] state) {
            this.key = key;
            this.serverSocket = (ServerSocket) state[0];
            this.accepted = (CountDownLatch) state[1];
            this.closedByClient = (CountDownLatch) state[2];
            this.sockets = (List<Socket>) state[3];
        }

        /**
         * The test class is loaded twice, by the JUnit class loader that builds the configuration and by the Quarkus class
         * loader that runs the test methods. Both copies must use the same server, so its state is kept in the system
         * properties, with JDK types only.
         */
        static SilentServer shared(String name) {
            String key = SocketReadIdleTimeoutTest.class.getName() + "." + name;
            Object[] state = (Object[]) System.getProperties().computeIfAbsent(key, k -> start());
            return new SilentServer(key, state);
        }

        private static Object[] start() {
            try {
                Object[] state = { new ServerSocket(0, 50, InetAddress.getLoopbackAddress()), new CountDownLatch(1),
                        new CountDownLatch(1), new CopyOnWriteArrayList<Socket>() };
                SilentServer server = new SilentServer(null, state);
                startDaemon(server::acceptLoop);
                return state;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        String url(String scheme) {
            return scheme + "://127.0.0.1:" + serverSocket.getLocalPort() + "/db";
        }

        private void acceptLoop() {
            while (!serverSocket.isClosed()) {
                try {
                    Socket socket = serverSocket.accept();
                    sockets.add(socket);
                    accepted.countDown();
                    startDaemon(() -> readUntilClosed(socket));
                } catch (IOException e) {
                    return;
                }
            }
        }

        // Reads and discards whatever the client sends, without ever answering
        private void readUntilClosed(Socket socket) {
            try (InputStream in = socket.getInputStream()) {
                while (in.read() != -1) {
                }
                closedByClient.countDown();
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    // connection reset by the client
                    closedByClient.countDown();
                }
            }
        }

        private static void startDaemon(Runnable runnable) {
            Thread thread = new Thread(runnable, "silent-server");
            thread.setDaemon(true);
            thread.start();
        }

        @Override
        public void close() {
            System.getProperties().remove(key);
            try {
                serverSocket.close();
                for (Socket socket : sockets) {
                    socket.close();
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
