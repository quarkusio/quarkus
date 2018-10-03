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
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;

/**
 * An output stream that writes data to a {@link java.net.DatagramSocket DatagramSocket}.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@SuppressWarnings("WeakerAccess")
public class UdpOutputStream extends OutputStream implements FlushableCloseable {
    private final DatagramSocket socket;
    private final SocketAddress socketAddress;

    public UdpOutputStream(final InetAddress address, final int port) throws IOException {
        this(ClientSocketFactory.of(address, port));
    }

    public UdpOutputStream(final ClientSocketFactory socketManager) throws SocketException {
        socket = socketManager.createDatagramSocket();
        socketAddress = socketManager.getSocketAddress();
    }

    @Override
    public void write(final int b) throws IOException {
        final byte[] msg = new byte[] {(byte) b};
        final DatagramPacket packet = new DatagramPacket(msg, 1, socketAddress);
        socket.send(packet);
    }

    @Override
    public void write(final byte[] b) throws IOException {
        if (b != null) {
            final DatagramPacket packet = new DatagramPacket(b, b.length, socketAddress);
            socket.send(packet);
        }
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        if (b != null) {
            final DatagramPacket packet = new DatagramPacket(b, off, len, socketAddress);
            socket.send(packet);
        }
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
