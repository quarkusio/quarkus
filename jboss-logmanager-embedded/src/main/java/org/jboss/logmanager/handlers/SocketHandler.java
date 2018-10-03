/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2015 Red Hat, Inc., and individual contributors
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
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.ExtLogRecord;

/**
 * A handler used to communicate over a socket.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class SocketHandler extends ExtHandler {

    /**
     * The type of socket
     */
    public enum Protocol {
        /**
         * Transmission Control Protocol
         */
        TCP,
        /**
         * User Datagram Protocol
         */
        UDP,
        /**
         * Transport Layer Security over TCP
         */
        SSL_TCP,
    }

    @SuppressWarnings("WeakerAccess")
    public static final int DEFAULT_PORT = 4560;

    private final Object outputLock = new Object();

    // All the following fields are guarded by outputLock
    private ClientSocketFactory clientSocketFactory;
    private SocketFactory socketFactory;
    private InetAddress address;
    private int port;
    private Protocol protocol;
    private boolean blockOnReconnect;
    private Writer writer;
    private boolean initialize;

    /**
     * Creates a socket handler with an address of {@linkplain java.net.InetAddress#getLocalHost() localhost} and port
     * of {@linkplain #DEFAULT_PORT 4560}.
     *
     * @throws UnknownHostException if an error occurs attempting to retrieve the localhost
     */
    public SocketHandler() throws UnknownHostException {
        this(InetAddress.getLocalHost(), DEFAULT_PORT);
    }

    /**
     * Creates a socket handler.
     *
     * @param hostname the hostname to connect to
     * @param port     the port to connect to
     *
     * @throws UnknownHostException if an error occurs resolving the address
     */
    public SocketHandler(final String hostname, final int port) throws UnknownHostException {
        this(InetAddress.getByName(hostname), port);
    }

    /**
     * Creates a socket handler.
     *
     * @param address the address to connect to
     * @param port    the port to connect to
     */
    public SocketHandler(final InetAddress address, final int port) {
        this(Protocol.TCP, address, port);
    }

    /**
     * Creates a socket handler.
     *
     * @param protocol the protocol to connect with
     * @param hostname the hostname to connect to
     * @param port     the port to connect to
     *
     * @throws UnknownHostException if an error occurs resolving the hostname
     */
    public SocketHandler(final Protocol protocol, final String hostname, final int port) throws UnknownHostException {
        this(protocol, InetAddress.getByName(hostname), port);
    }

    /**
     * Creates a socket handler.
     *
     * @param protocol the protocol to connect with
     * @param address  the address to connect to
     * @param port     the port to connect to
     */
    public SocketHandler(final Protocol protocol, final InetAddress address, final int port) {
        this(null, protocol, address, port);
    }

    /**
     * Creates a socket handler.
     *
     * @param socketFactory the socket factory to use for creating {@linkplain Protocol#TCP TCP} or
     *                      {@linkplain Protocol#SSL_TCP SSL TCP} connections, if {@code null} a default factory will
     *                      be used
     * @param protocol      the protocol to connect with
     * @param hostname      the hostname to connect to
     * @param port          the port to connect to
     *
     * @throws UnknownHostException if an error occurs resolving the hostname
     * @see #SocketHandler(ClientSocketFactory, Protocol)
     */
    public SocketHandler(final SocketFactory socketFactory, final Protocol protocol, final String hostname, final int port) throws UnknownHostException {
        this(socketFactory, protocol, InetAddress.getByName(hostname), port);
    }

    /**
     * Creates a socket handler.
     *
     * @param socketFactory the socket factory to use for creating {@linkplain Protocol#TCP TCP} or
     *                      {@linkplain Protocol#SSL_TCP SSL TCP} connections, if {@code null} a default factory will
     *                      be used
     * @param protocol      the protocol to connect with
     * @param address       the address to connect to
     * @param port          the port to connect to
     *
     * @see #SocketHandler(ClientSocketFactory, Protocol)
     */
    public SocketHandler(final SocketFactory socketFactory, final Protocol protocol, final InetAddress address, final int port) {
        this.socketFactory = socketFactory;
        this.clientSocketFactory = null;
        this.address = address;
        this.port = port;
        this.protocol = (protocol == null ? Protocol.TCP : protocol);
        initialize = true;
        writer = null;
        blockOnReconnect = false;
    }

    /**
     * Creates a socket handler.
     *
     * @param clientSocketFactory the client socket factory used to create sockets
     * @param protocol            the protocol to connect with
     */
    public SocketHandler(final ClientSocketFactory clientSocketFactory, final Protocol protocol) {
        this.clientSocketFactory = clientSocketFactory;
        if (clientSocketFactory != null) {
            address = clientSocketFactory.getAddress();
            port = clientSocketFactory.getPort();
        }
        this.protocol = (protocol == null ? Protocol.TCP : protocol);
        initialize = true;
        writer = null;
        blockOnReconnect = false;
    }

    @Override
    protected void doPublish(final ExtLogRecord record) {
        final String formatted;
        final Formatter formatter = getFormatter();
        try {
            formatted = formatter.format(record);
        } catch (Exception e) {
            reportError("Could not format message", e, ErrorManager.FORMAT_FAILURE);
            return;
        }
        if (formatted.isEmpty()) {
            // nothing to write; move along
            return;
        }
        try {
            synchronized (outputLock) {
                if (initialize) {
                    initialize();
                    initialize = false;
                }
                if (writer == null) {
                    return;
                }
                writer.write(formatted);
                super.doPublish(record);
            }
        } catch (Exception e) {
            reportError("Error writing log message", e, ErrorManager.WRITE_FAILURE);
        }
    }

    @Override
    public void flush() {
        synchronized (outputLock) {
            safeFlush(writer);
        }
        super.flush();
    }

    @Override
    public void close() throws SecurityException {
        synchronized (outputLock) {
            safeClose(writer);
            writer = null;
            initialize = true;
        }
        super.close();
    }

    /**
     * Returns the address being used.
     *
     * @return the address
     */
    public InetAddress getAddress() {
        return address;
    }

    /**
     * Sets the address to connect to.
     * <p>
     * Note that is resets the {@linkplain #setClientSocketFactory(ClientSocketFactory) client socket factory}.
     * </p>
     *
     * @param address the address
     */
    public void setAddress(final InetAddress address) {
        synchronized (outputLock) {
            if (!this.address.equals(address)) {
                initialize = true;
                clientSocketFactory = null;
            }
            this.address = address;
        }
    }

    /**
     * Sets the address to connect to by doing a lookup on the hostname.
     * <p>
     * Note that is resets the {@linkplain #setClientSocketFactory(ClientSocketFactory) client socket factory}.
     * </p>
     *
     * @param hostname the host name used to resolve the address
     *
     * @throws UnknownHostException if an error occurs resolving the address
     */
    public void setHostname(final String hostname) throws UnknownHostException {
        setAddress(InetAddress.getByName(hostname));
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
     * Enables or disables blocking when attempting to reconnect the socket when using a {@linkplain Protocol#TCP TCP}
     * or {@linkplain Protocol#SSL_TCP SSL TCP} connections.
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
            initialize = true;
        }
    }

    /**
     * Returns the protocol being used.
     *
     * @return the protocol
     */
    public Protocol getProtocol() {
        return protocol;
    }

    /**
     * Sets the protocol to use. If the value is {@code null} the protocol will be set to
     * {@linkplain Protocol#TCP TCP}.
     * <p>
     * Note that is resets the {@linkplain #setSocketFactory(SocketFactory) socket factory} if it was previously set.
     * </p>
     *
     * @param protocol the protocol to use
     */
    public void setProtocol(final Protocol protocol) {
        synchronized (outputLock) {
            if (protocol == null) {
                this.protocol = Protocol.TCP;
            }
            if (this.protocol != protocol) {
                socketFactory = null;
                initialize = true;
            }
            this.protocol = protocol;
        }
    }

    /**
     * Returns the port being used.
     *
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * Sets the port to connect to.
     * <p>
     * Note that is resets the {@linkplain #setClientSocketFactory(ClientSocketFactory) client socket factory}.
     * </p>
     *
     * @param port the port
     */
    public void setPort(final int port) {
        synchronized (outputLock) {
            if (this.port != port) {
                initialize = true;
                clientSocketFactory = null;
            }
            this.port = port;
        }
    }

    /**
     * Sets the socket factory to use for creating {@linkplain Protocol#TCP TCP} or {@linkplain Protocol#SSL_TCP SSL}
     * connections.
     * <p>
     * Note that if the {@linkplain #setProtocol(Protocol) protocol} is set the socket factory will be set to
     * {@code null} and reset. Setting a value here also resets the
     * {@linkplain #setClientSocketFactory(ClientSocketFactory) client socket factory}.
     * </p>
     *
     * @param socketFactory the socket factory
     *
     * @see #setClientSocketFactory(ClientSocketFactory)
     */
    public void setSocketFactory(final SocketFactory socketFactory) {
        synchronized (outputLock) {
            this.socketFactory = socketFactory;
            this.clientSocketFactory = null;
            initialize = true;
        }
    }

    /**
     * Sets the client socket factory used to create sockets. If {@code null} the
     * {@linkplain #setAddress(InetAddress) address} and {@linkplain #setPort(int) port} are required to be set.
     *
     * @param clientSocketFactory the client socket factory to use
     */
    public void setClientSocketFactory(final ClientSocketFactory clientSocketFactory) {
        synchronized (outputLock) {
            this.clientSocketFactory = clientSocketFactory;
            initialize = true;
        }
    }

    private void initialize() {
        final Writer current = this.writer;
        boolean okay = false;
        try {
            if (current != null) {
                writeTail(current);
                safeFlush(current);
            }
            // Close the current writer before we attempt to create a new connection
            safeClose(current);
            final OutputStream out = createOutputStream();
            if (out == null) {
                return;
            }
            final String encoding = getEncoding();
            final UninterruptibleOutputStream outputStream = new UninterruptibleOutputStream(out);
            if (encoding == null) {
                writer = new OutputStreamWriter(outputStream);
            } else {
                writer = new OutputStreamWriter(outputStream, encoding);
            }
            writeHead(writer);
            okay = true;
        } catch (UnsupportedEncodingException e) {
            reportError("Error opening", e, ErrorManager.OPEN_FAILURE);
        } finally {
            safeClose(current);
            if (!okay) {
                safeClose(writer);
            }
        }

    }

    private OutputStream createOutputStream() {
        if (address != null || port >= 0) {
            try {
                final ClientSocketFactory socketFactory = getClientSocketFactory();
                if (protocol == Protocol.UDP) {
                    return new UdpOutputStream(socketFactory);
                }
                return new TcpOutputStream(socketFactory, blockOnReconnect);
            } catch (IOException e) {
                reportError("Failed to create socket output stream", e, ErrorManager.OPEN_FAILURE);
            }
        }
        return null;
    }

    private ClientSocketFactory getClientSocketFactory() {
        synchronized (outputLock) {
            if (clientSocketFactory != null) {
                return clientSocketFactory;
            }
            if (address == null || port <= 0) {
                throw new IllegalStateException("An address and port greater than 0 is required.");
            }
            final ClientSocketFactory clientSocketFactory;
            if (socketFactory == null) {
                if (protocol == Protocol.SSL_TCP) {
                    clientSocketFactory = ClientSocketFactory.of(SSLSocketFactory.getDefault(), address, port);
                } else {
                    clientSocketFactory = ClientSocketFactory.of(address, port);
                }
            } else {
                clientSocketFactory = ClientSocketFactory.of(socketFactory, address, port);
            }
            return clientSocketFactory;
        }
    }

    private void writeHead(final Writer writer) {
        try {
            final Formatter formatter = getFormatter();
            if (formatter != null) writer.write(formatter.getHead(this));
        } catch (Exception e) {
            reportError("Error writing section header", e, ErrorManager.WRITE_FAILURE);
        }
    }

    private void writeTail(final Writer writer) {
        try {
            final Formatter formatter = getFormatter();
            if (formatter != null) writer.write(formatter.getTail(this));
        } catch (Exception ex) {
            reportError("Error writing section tail", ex, ErrorManager.WRITE_FAILURE);
        }
    }

    private void safeClose(Closeable c) {
        try {
            if (c != null) c.close();
        } catch (Exception e) {
            reportError("Error closing resource", e, ErrorManager.CLOSE_FAILURE);
        } catch (Throwable ignored) {
        }
    }

    private void safeFlush(Flushable f) {
        try {
            if (f != null) f.flush();
        } catch (Exception e) {
            reportError("Error on flush", e, ErrorManager.FLUSH_FAILURE);
        } catch (Throwable ignored) {
        }
    }
}