/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.logmanager.handlers;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.net.SocketFactory;

/**
 * An output stream that writes data to a {@link java.net.Socket socket}.
 * <p/>
 * If an {@link java.io.IOException IOException} occurs during a {@link #write(byte[], int, int)} and a {@link
 * javax.net.SocketFactory socket factory} was defined the stream will attempt to reconnect indefinitely. By default
 * additional writes are discarded when reconnecting. If you set the {@link #setBlockOnReconnect(boolean) block on
 * reconnect} to {@code true}, then the reconnect will indefinitely block until the TCP stream is reconnected.
 * <p/>
 * You can optionally get a collection of the errors that occurred during a write or reconnect.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class TcpOutputStream extends OutputStream implements FlushableCloseable {
    private static final long retryTimeout = 5L;
    private static final long maxRetryTimeout = 40L;
    private static final int maxErrors = 10;

    protected final Object outputLock = new Object();

    private final ClientSocketFactory socketFactory;
    private final Deque<Exception> errors = new ArrayDeque<Exception>(maxErrors);

    // Guarded by outputLock
    private Thread reconnectThread;
    // Guarded by outputLock
    private boolean blockOnReconnect;
    // Guarded by outputLock
    private Socket socket;
    // Guarded by outputLock
    private boolean connected;

    /**
     * Creates a TCP output stream.
     * <p/>
     * Uses the {@link javax.net.SocketFactory#getDefault() default socket factory} to create the socket.
     *
     * @param address the address to connect to
     * @param port    the port to connect to
     *
     * @throws IOException no longer throws an exception. If an exception occurs while attempting to connect the socket
     *                     a reconnect will be attempted on the next write.
     */
    public TcpOutputStream(final InetAddress address, final int port) throws IOException {
        this(SocketFactory.getDefault(), address, port);
    }

    /**
     * Creates a TCP output stream.
     * <p>
     * Uses the {@link javax.net.SocketFactory#getDefault() default socket factory} to create the socket.
     * </p>
     *
     * @param address          the address to connect to
     * @param port             the port to connect to
     * @param blockOnReconnect {@code true} to block when attempting to reconnect the socket or {@code false} to
     *                         reconnect asynchronously
     *
     * @throws IOException no longer throws an exception. If an exception occurs while attempting to connect the socket
     *                     a reconnect will be attempted on the next write.
     */
    public TcpOutputStream(final InetAddress address, final int port, final boolean blockOnReconnect) throws IOException {
        this(SocketFactory.getDefault(), address, port, blockOnReconnect);
    }

    /**
     * Creates a new TCP output stream.
     * <p/>
     * <strong>Using this constructor does not allow for any kind of reconnect if the connection is dropped.</strong>
     *
     * @param socket the socket used to write the output to
     *
     * @deprecated Use {@link #TcpOutputStream(ClientSocketFactory, boolean)}
     */
    @Deprecated
    protected TcpOutputStream(final Socket socket) {
        this.socketFactory = null;
        this.socket = socket;
        reconnectThread = null;
        connected = true;
    }

    /**
     * Creates a new TCP output stream.
     * <p/>
     * Creates a {@link java.net.Socket socket} from the {@code socketFactory} argument.
     *
     * @param socketFactory the factory used to create the socket
     * @param address       the address to connect to
     * @param port          the port to connect to
     *
     * @throws IOException no longer throws an exception. If an exception occurs while attempting to connect the socket
     *                     a reconnect will be attempted on the next write.
     */
    protected TcpOutputStream(final SocketFactory socketFactory, final InetAddress address, final int port) throws IOException {
        this(socketFactory, address, port, false);
    }

    /**
     * Creates a new TCP output stream.
     * <p>
     * Creates a {@link java.net.Socket socket} from the {@code socketFactory} argument.
     * </p>
     *
     * @param socketFactory    the factory used to create the socket
     * @param address          the address to connect to
     * @param port             the port to connect to
     * @param blockOnReconnect {@code true} to block when attempting to reconnect the socket or {@code false} to
     *                         reconnect asynchronously
     *
     * @throws IOException no longer throws an exception. If an exception occurs while attempting to connect the socket
     *                     a reconnect will be attempted on the next write.
     */
    protected TcpOutputStream(final SocketFactory socketFactory, final InetAddress address, final int port, final boolean blockOnReconnect) throws IOException {
        this(ClientSocketFactory.of(socketFactory, address, port), blockOnReconnect);
    }

    /**
     * Creates a new TCP stream which uses the {@link ClientSocketFactory#createSocket()} to create the socket.
     *
     * @param socketFactory    the socket factory used to create TCP sockets
     * @param blockOnReconnect {@code true} to block when attempting to reconnect the socket or {@code false} to
     *                         reconnect asynchronously
     */
    public TcpOutputStream(final ClientSocketFactory socketFactory, final boolean blockOnReconnect) {
        this.socketFactory = socketFactory;
        this.blockOnReconnect = blockOnReconnect;
        try {
            socket = this.socketFactory.createSocket();
            connected = true;
        } catch (IOException e) {
            connected = false;
        }
    }

    @Override
    public void write(final int b) throws IOException {
        write(new byte[] {(byte) b}, 0, 1);
    }

    @Override
    public void write(final byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        synchronized (outputLock) {
            try {
                checkReconnect();
                if (connected) {
                    socket.getOutputStream().write(b, off, len);
                }
            } catch (SocketException e) {
                if (isReconnectAllowed()) {
                    // Close the previous socket
                    safeClose(socket);
                    connected = false;
                    addError(e);
                    // Handle the reconnection
                    reconnectThread = createThread();
                    if (blockOnReconnect) {
                        reconnectThread.run();
                        // We should be reconnected, try to write again
                        write(b, off, len);
                    } else {
                        reconnectThread.start();
                    }
                } else {
                    throw e;
                }
            }
        }
    }

    @Override
    public void flush() throws IOException {
        synchronized (outputLock) {
            try {
                if (socket != null) {
                    socket.getOutputStream().flush();
                }
            } catch (SocketException e) {
                // This should likely never be hit, but should attempt to reconnect if it does happen
                if (isReconnectAllowed()) {
                    // Close the previous socket
                    safeClose(socket);
                    // Reconnection should be attempted on the next write if allowed
                    connected = false;
                    addError(e);
                } else {
                    throw e;
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (outputLock) {
            if (reconnectThread != null) {
                reconnectThread.interrupt();
            }
            if (socket != null) {
                socket.close();
            }
        }
    }

    /**
     * Indicates whether or not the output stream is set to block when attempting to reconnect a TCP connection.
     *
     * @return {@code true} if blocking is enabled, otherwise {@code false}
     */
    public boolean isBlockOnReconnect() {
        synchronized (outputLock) {
            return blockOnReconnect;
        }
    }

    /**
     * Enables or disables blocking when attempting to reconnect the socket.
     * <p/>
     * If set to {@code true} the {@code write} methods will block when attempting to reconnect. This is only advisable
     * to be set to {@code true} if using an asynchronous handler.
     *
     * @param blockOnReconnect {@code true} to block when reconnecting or {@code false} to reconnect asynchronously
     *                         discarding any new messages coming in
     */
    public void setBlockOnReconnect(final boolean blockOnReconnect) {
        synchronized (outputLock) {
            this.blockOnReconnect = blockOnReconnect;
        }
    }

    /**
     * Returns the connected state of the TCP stream.
     * <p/>
     * The stream is said to be disconnected when an {@link java.io.IOException} occurs during a write. Otherwise a
     * stream is considered connected.
     *
     * @return {@code true} if the stream is connected, otherwise {@code false}
     */
    public boolean isConnected() {
        synchronized (outputLock) {
            return connected;
        }
    }

    /**
     * Retrieves the errors occurred, if any, during a write or reconnect.
     *
     * @return a collection of errors or an empty list
     */
    public Collection<Exception> getErrors() {
        synchronized (errors) {
            if (!errors.isEmpty()) {
                // drain the errors and return a list
                final List<Exception> result = new ArrayList<Exception>(errors);
                errors.clear();
                return result;
            }
        }
        return Collections.emptyList();
    }

    private void addError(final Exception e) {
        synchronized (errors) {
            if (errors.size() < maxErrors) {
                errors.addLast(e);
            }
            // TODO (jrp) should we do something with these errors
        }
    }

    /**
     * Invocations of this method must be locked by the {@link #outputLock}.
     */
    private boolean isReconnectAllowed() {
        return socketFactory != null && reconnectThread == null;
    }

    /**
     * Attempts to reconnect the socket if required and allowed. Invocations of this method must be locked by the
     * {@link #outputLock}.
     */
    private void checkReconnect() {
        if (!connected && isReconnectAllowed()) {
            reconnectThread = createThread();
            if (blockOnReconnect) {
                reconnectThread.run();
            } else {
                reconnectThread.start();
            }
        }
    }

    private Thread createThread() {
        final Thread thread = new Thread(new RetryConnector());
        thread.setDaemon(true);
        thread.setName("LogManager Socket Reconnect Thread");
        return thread;
    }

    private static void safeClose(final Closeable closeable) {
        if (closeable != null) try {
            closeable.close();
        } catch (Exception ignore) {
        }
    }

    private class RetryConnector implements Runnable {
        private int attempts = 0;

        @Override
        public void run() {
            boolean connected = false;
            while (socketFactory != null && !connected) {
                Socket socket = null;
                try {
                    socket = socketFactory.createSocket();
                    synchronized (outputLock) {
                        // Unlikely but if we've been interrupted due to a close, we should shutdown
                        if (Thread.currentThread().isInterrupted()) {
                            safeClose(socket);
                            break;
                        } else {
                            TcpOutputStream.this.socket = socket;
                            TcpOutputStream.this.connected = true;
                            TcpOutputStream.this.reconnectThread = null;
                            connected = true;
                        }
                    }
                } catch (IOException e) {
                    connected = false;
                    addError(e);
                    final long timeout;
                    if (attempts++ > 0L) {
                        timeout = (10 * attempts);
                    } else {
                        timeout = retryTimeout;
                    }
                    // Wait for a bit, then try to reconnect
                    try {
                        TimeUnit.SECONDS.sleep(Math.min(timeout, maxRetryTimeout));
                    } catch (InterruptedException ignore) {
                        synchronized (outputLock) {
                            TcpOutputStream.this.connected = false;
                        }
                        break;
                    }
                } finally {
                    // It's possible the thread was interrupted, if we're not connected we should clean up the socket
                    if (!connected) {
                        safeClose(socket);
                    }
                }
            }
        }
    }
}
