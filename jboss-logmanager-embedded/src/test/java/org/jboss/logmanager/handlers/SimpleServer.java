package org.jboss.logmanager.handlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.GeneralSecurityException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
abstract class SimpleServer implements Runnable, AutoCloseable {

    private final BlockingQueue<String> data;
    private final ExecutorService service;

    private SimpleServer(final BlockingQueue<String> data) {
        this.data = data;
        service = Executors.newSingleThreadExecutor(r -> {
            final Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        });
    }

    static SimpleServer createTcpServer(final int port) throws IOException {
        final SimpleServer server = new TcpServer(ServerSocketFactory.getDefault(), new LinkedBlockingDeque<>(), port);
        server.start();
        return server;
    }


    static SimpleServer createTlsServer(final int port) throws IOException, GeneralSecurityException {
        final SimpleServer server = new TcpServer(SSLServerSocketFactory.getDefault(), new LinkedBlockingDeque<>(), port);
        server.start();
        return server;
    }

    static SimpleServer createUdpServer(final int port) throws IOException {
        final SimpleServer server = new UdpServer(new LinkedBlockingDeque<>(), port);
        server.start();
        return server;
    }

    String timeoutPoll() throws InterruptedException {
        return data.poll(10, TimeUnit.SECONDS);
    }

    String poll() throws InterruptedException {
        return data.poll();
    }

    String peek() {
        return data.peek();
    }

    private void start() {
        service.submit(this);
    }

    @Override
    public void close() throws Exception {
        service.shutdown();
        service.awaitTermination(30, TimeUnit.SECONDS);
    }

    private static class TcpServer extends SimpleServer {
        private final BlockingQueue<String> data;
        private final AtomicBoolean closed = new AtomicBoolean(true);
        private final ServerSocket serverSocket;
        private volatile Socket socket;

        private TcpServer(final ServerSocketFactory serverSocketFactory, final BlockingQueue<String> data, final int port) throws IOException {
            super(data);
            this.serverSocket = serverSocketFactory.createServerSocket(port);
            this.data = data;
        }

        @Override
        public void run() {
            closed.set(false);
            try {
                socket = serverSocket.accept();
                InputStream in = socket.getInputStream();
                final ByteArrayOutputStream out = new ByteArrayOutputStream();
                while (!closed.get()) {
                    final byte[] buffer = new byte[512];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        final byte lastByte = buffer[len - 1];
                        if (lastByte == '\n') {
                            out.write(buffer, 0, (len - 1));
                            data.put(out.toString());
                            out.reset();
                        } else {
                            out.write(buffer, 0, len);
                        }
                    }
                }
            } catch (IOException e) {
                if (!closed.get()) {
                    throw new UncheckedIOException(e);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void close() throws Exception {
            try {
                closed.set(true);
                try {
                    socket.close();
                } finally {
                    serverSocket.close();
                }
            } finally {
                super.close();
            }
        }
    }

    private static class UdpServer extends SimpleServer {
        private final BlockingQueue<String> data;
        private final AtomicBoolean closed = new AtomicBoolean(true);
        private final DatagramSocket socket;

        private UdpServer(final BlockingQueue<String> data, final int port) throws SocketException {
            super(data);
            this.data = data;
            socket = new DatagramSocket(port);
        }

        @Override
        public void run() {
            closed.set(false);
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            while (!closed.get()) {
                try {
                    final DatagramPacket packet = new DatagramPacket(new byte[2048], 2048);
                    socket.receive(packet);
                    final int len = packet.getLength();
                    byte[] bytes = new byte[len];
                    System.arraycopy(packet.getData(), 0, bytes, 0, len);
                    final byte lastByte = bytes[len - 1];
                    if (lastByte == '\n') {
                        out.write(bytes, 0, (len - 1));
                        data.put(out.toString());
                        out.reset();
                    } else {
                        out.write(bytes, 0, len);
                    }
                } catch (IOException e) {
                    if (!closed.get()) {
                        throw new UncheckedIOException(e);
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @Override
        public void close() throws Exception {
            try {
                closed.set(true);
                socket.close();
            } finally {
                super.close();
            }
        }
    }
}
