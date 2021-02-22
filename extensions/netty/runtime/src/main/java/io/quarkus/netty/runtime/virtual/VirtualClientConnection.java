package io.quarkus.netty.runtime.virtual;

import java.net.SocketAddress;

import io.netty.channel.Channel;
import io.netty.util.concurrent.Future;
import io.netty.util.internal.PlatformDependent;

/**
 * A virtual client connection to an intra-JVM request/response netty server channel.
 */
public class VirtualClientConnection<T> {
    protected final SocketAddress clientAddress;
    protected boolean connected = true;
    protected VirtualChannel peer;
    protected final VirtualResponseHandler handler;

    public VirtualClientConnection(SocketAddress clientAddress, VirtualResponseHandler handler) {
        this.clientAddress = clientAddress;
        this.handler = handler;
    }

    public SocketAddress clientAddress() {
        return clientAddress;
    }

    public void close() {
        // todo more cleanup?
        connected = false;
        peer.close();
        handler.close();
    }

    public boolean isConnected() {
        return connected;
    }

    /**
     * Send a message directly to the server connection's event loop
     *
     * @param msg
     */
    public void sendMessage(Object msg) {
        peer.inboundBuffer.add(msg);
        finishPeerRead0(peer);
    }

    private void finishPeerRead0(VirtualChannel peer) {
        Future<?> peerFinishReadFuture = peer.finishReadFuture;
        if (peerFinishReadFuture != null) {
            if (!peerFinishReadFuture.isDone()) {
                runFinishPeerReadTask(peer);
                return;
            } else {
                // Lazy unset to make sure we don't prematurely unset it while scheduling a new task.
                VirtualChannel.FINISH_READ_FUTURE_UPDATER.compareAndSet(peer, peerFinishReadFuture, null);
            }
        }
        // We should only set readInProgress to false if there is any data that was read as otherwise we may miss to
        // forward data later on.
        if (peer.readInProgress && !peer.inboundBuffer.isEmpty()) {
            peer.readInProgress = false;
            peer.readInbound();
        }
    }

    private void runFinishPeerReadTask(final VirtualChannel peer) {
        // If the peer is writing, we must wait until after reads are completed for that peer before we can read. So
        // we keep track of the task, and coordinate later that our read can't happen until the peer is done.
        final Runnable finishPeerReadTask = new Runnable() {
            @Override
            public void run() {
                finishPeerRead0(peer);
            }
        };
        try {
            if (peer.writeInProgress) {
                peer.finishReadFuture = peer.eventLoop().submit(finishPeerReadTask);
            } else {
                peer.eventLoop().execute(finishPeerReadTask);
            }
        } catch (Throwable cause) {
            close();
            peer.close();
            PlatformDependent.throwException(cause);
        }
    }

    /**
     * Establish a virtual intra-JVM connection
     *
     * @param remoteAddress
     * @return
     */
    public static VirtualClientConnection connect(VirtualResponseHandler handler, VirtualAddress remoteAddress) {
        return connect(handler, remoteAddress, remoteAddress);
    }

    /**
     * Establish a virtual intra-JVM connection
     *
     * @param remoteAddress
     * @param clientAddress
     * @return
     */
    public static VirtualClientConnection connect(VirtualResponseHandler handler, VirtualAddress remoteAddress,
            SocketAddress clientAddress) {
        if (clientAddress == null)
            clientAddress = remoteAddress;

        Channel boundChannel = VirtualChannelRegistry.get(remoteAddress);
        if (boundChannel == null) {
            throw new RuntimeException("No virtual channel available");
        }
        if (!(boundChannel instanceof VirtualServerChannel)) {
            throw new RuntimeException("Should be virtual server channel: " + boundChannel.getClass().getName());
        }

        VirtualServerChannel serverChannel = (VirtualServerChannel) boundChannel;
        VirtualClientConnection conn = new VirtualClientConnection(clientAddress, handler);
        conn.peer = serverChannel.serve(conn);
        return conn;
    }
}
