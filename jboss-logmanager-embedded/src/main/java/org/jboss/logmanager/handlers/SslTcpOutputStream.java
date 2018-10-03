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

import java.io.IOException;
import java.net.InetAddress;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

/**
 * An output stream that writes data to a {@link java.net.Socket socket}. Uses {@link
 * javax.net.ssl.SSLSocketFactory#getDefault()} to create the socket.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class SslTcpOutputStream extends TcpOutputStream implements FlushableCloseable {

    /**
     * Creates a SSL TCP output stream.
     * <p/>
     * Uses the {@link javax.net.ssl.SSLSocketFactory#getDefault() default socket factory} to create the socket.
     *
     * @param address the address to connect to
     * @param port    the port to connect to
     *
     * @throws IOException if an I/O error occurs when creating the socket
     */
    public SslTcpOutputStream(final InetAddress address, final int port) throws IOException {
        super(SSLSocketFactory.getDefault(), address, port);
    }

    /**
     * Creates a SSL TCP output stream.
     * <p/>
     * Uses the {@link javax.net.ssl.SSLSocketFactory#getDefault() default socket factory} to create the socket.
     *
     * @param socketFactory the factory used to create the socket
     * @param address       the address to connect to
     * @param port          the port to connect to
     *
     * @throws IOException if an I/O error occurs when creating the socket
     */
    public SslTcpOutputStream(final SocketFactory socketFactory, final InetAddress address, final int port) throws IOException {
        super(socketFactory, address, port);
    }

    /**
     * Creates a SSL TCP output stream.
     * <p/>
     * Uses the {@link javax.net.ssl.SSLSocketFactory#getDefault() default socket factory} to create the socket.
     *
     * @param address          the address to connect to
     * @param port             the port to connect to
     * @param blockOnReconnect {@code true} to block when attempting to reconnect the socket or {@code false} to
     *                         reconnect asynchronously
     *
     * @throws IOException if an I/O error occurs when creating the socket
     */
    public SslTcpOutputStream(final InetAddress address, final int port, final boolean blockOnReconnect) throws IOException {
        super(SSLSocketFactory.getDefault(), address, port, blockOnReconnect);
    }

    /**
     * Creates a SSL TCP output stream.
     * <p/>
     * Uses the {@link javax.net.ssl.SSLSocketFactory#getDefault() default socket factory} to create the socket.
     *
     * @param socketFactory    the factory used to create the socket
     * @param address          the address to connect to
     * @param port             the port to connect to
     * @param blockOnReconnect {@code true} to block when attempting to reconnect the socket or {@code false} to
     *                         reconnect asynchronously
     *
     * @throws IOException if an I/O error occurs when creating the socket
     */
    public SslTcpOutputStream(final SocketFactory socketFactory, final InetAddress address, final int port, final boolean blockOnReconnect) throws IOException {
        super(socketFactory, address, port, blockOnReconnect);
    }
}
