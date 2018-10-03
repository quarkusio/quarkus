/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2018 Red Hat, Inc., and individual contributors
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
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import javax.net.SocketFactory;

/**
 * A factory used to create writable sockets.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public interface ClientSocketFactory {

    /**
     * Creates a datagram socket for UDP communication.
     *
     * @return the newly created socket
     *
     * @throws SocketException if binding the socket fails
     */
    DatagramSocket createDatagramSocket() throws SocketException;

    /**
     * Creates a TCP socket.
     *
     * @return the newly created socket
     *
     * @throws IOException if an error occurs creating the socket
     */
    Socket createSocket() throws IOException;

    /**
     * Returns the address being used to create sockets.
     *
     * @return the address being used
     */
    InetAddress getAddress();

    /**
     * Returns the port being used to create sockets.
     *
     * @return the port being used
     */
    int getPort();

    /**
     * A convenience method to return the socket address.
     * <p>
     * The default implementation simply returns {@code new InetSocketAddress(getAddress(), getPort())}.
     * </p>
     *
     * @return a socket address
     */
    default SocketAddress getSocketAddress() {
        return new InetSocketAddress(getAddress(), getPort());
    }

    /**
     * Creates a new default implementation of the factory which uses {@link SocketFactory#getDefault()} for TCP
     * sockets and {@code new DatagramSocket()} for UDP sockets.
     *
     * @param address the address to bind to
     * @param port    the port to bind to
     *
     * @return the client socket factory
     */
    static ClientSocketFactory of(final InetAddress address, final int port) {
        return of(SocketFactory.getDefault(), address, port);
    }

    /**
     * Creates a new default implementation of the factory which uses the provided
     * {@linkplain SocketFactory#createSocket(InetAddress, int) socket factory} to create TCP connections and
     * {@code new DatagramSocket()} for UDP sockets.
     *
     * @param socketFactory the socket factory used for TCP connections, if {@code null} the
     *                      {@linkplain SocketFactory#getDefault() default} socket factory will be used
     * @param address       the address to bind to
     * @param port          the port to bind to
     *
     * @return the client socket factory
     */
    static ClientSocketFactory of(final SocketFactory socketFactory, final InetAddress address, final int port) {
        if (address == null || port < 0) {
            throw new IllegalArgumentException(String.format("The address cannot be null (%s) and the port must be a positive integer (%d)", address, port));
        }
        final SocketFactory factory = (socketFactory == null ? SocketFactory.getDefault() : socketFactory);
        return new ClientSocketFactory() {
            @Override
            public DatagramSocket createDatagramSocket() throws SocketException {
                return new DatagramSocket();
            }

            @Override
            public Socket createSocket() throws IOException {
                return factory.createSocket(address, port);
            }

            @Override
            public InetAddress getAddress() {
                return address;
            }

            @Override
            public int getPort() {
                return port;
            }
        };
    }
}
