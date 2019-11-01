/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.quarkus.netty.runtime.virtual;

import java.net.SocketAddress;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ConnectionPendingException;
import java.nio.channels.NotYetConnectedException;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import io.netty.channel.AbstractChannel;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.EventLoop;
import io.netty.channel.PreferHeapByteBufAllocator;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.SingleThreadEventLoop;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.SingleThreadEventExecutor;
import io.netty.util.internal.InternalThreadLocalMap;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * A {@link Channel} for the local transport. This is a bit different than a LocalChannel in regular Netty
 * as it does not require a client event loop and exposes the client inbound queue directly.
 *
 * The queue exposed is a blocking queue so that local virtual clients can block on this queue and obtain
 * messages directly for processing.
 */
public class VirtualChannel extends AbstractChannel {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(VirtualChannel.class);
    @SuppressWarnings({ "rawtypes" })
    protected static final AtomicReferenceFieldUpdater<VirtualChannel, Future> FINISH_READ_FUTURE_UPDATER = AtomicReferenceFieldUpdater
            .newUpdater(VirtualChannel.class, Future.class, "finishReadFuture");
    private static final ChannelMetadata METADATA = new ChannelMetadata(false);
    private static final int MAX_READER_STACK_DEPTH = 8;

    private enum State {
        OPEN,
        BOUND,
        CONNECTED,
        CLOSED
    }

    private final ChannelConfig config = new DefaultChannelConfig(this);
    // To further optimize this we could sendMessage our own SPSC queue.
    final Queue<Object> inboundBuffer = PlatformDependent.newSpscQueue();
    final VirtualClientConnection virtualConnection;
    private final Runnable readTask = new Runnable() {
        @Override
        public void run() {
            // Ensure the inboundBuffer is not empty as readInbound() will always call fireChannelReadComplete()
            if (!inboundBuffer.isEmpty()) {
                readInbound();
            }
        }
    };

    private final Runnable shutdownHook = new Runnable() {
        @Override
        public void run() {
            unsafe().close(unsafe().voidPromise());
        }
    };

    protected volatile State state;
    protected volatile VirtualAddress localAddress;
    protected volatile SocketAddress remoteAddress;
    protected volatile ChannelPromise connectPromise;
    protected volatile boolean readInProgress;
    protected volatile boolean writeInProgress;
    protected volatile Future<?> finishReadFuture;

    protected VirtualChannel(VirtualServerChannel parent, VirtualClientConnection connection) {
        super(parent);
        config().setAllocator(new PreferHeapByteBufAllocator(config.getAllocator()));
        localAddress = parent.localAddress();
        remoteAddress = connection.clientAddress();
        this.virtualConnection = connection;
    }

    @Override
    public ChannelMetadata metadata() {
        return METADATA;
    }

    @Override
    public ChannelConfig config() {
        return config;
    }

    @Override
    public VirtualServerChannel parent() {
        return (VirtualServerChannel) super.parent();
    }

    @Override
    public VirtualAddress localAddress() {
        return (VirtualAddress) super.localAddress();
    }

    @Override
    public SocketAddress remoteAddress() {
        return remoteAddress;
    }

    @Override
    public boolean isOpen() {
        return state != State.CLOSED;
    }

    @Override
    public boolean isActive() {
        return state == State.CONNECTED;
    }

    @Override
    protected AbstractUnsafe newUnsafe() {
        return new LocalUnsafe();
    }

    @Override
    protected boolean isCompatible(EventLoop loop) {
        return loop instanceof SingleThreadEventLoop;
    }

    @Override
    protected SocketAddress localAddress0() {
        return localAddress;
    }

    @Override
    protected SocketAddress remoteAddress0() {
        return remoteAddress;
    }

    @Override
    protected void doRegister() throws Exception {
        // Check if both peer and parent are non-null because this channel was created by a LocalServerChannel.
        // This is needed as a peer may not be null also if a LocalChannel was connected before and
        // deregistered / registered later again.
        //
        // See https://github.com/netty/netty/issues/2400
        if (parent() != null) {
            // Store the peer in a local variable as it may be set to null if doClose() is called.
            // See https://github.com/netty/netty/issues/2144
            state = State.CONNECTED;
        }
        ((SingleThreadEventExecutor) eventLoop()).addShutdownHook(shutdownHook);
    }

    @Override
    protected void doBind(SocketAddress localAddress) throws Exception {
        this.localAddress = VirtualChannelRegistry.register(this, this.localAddress,
                localAddress);
        state = State.BOUND;
    }

    @Override
    protected void doDisconnect() throws Exception {
        doClose();
    }

    @Override
    protected void doClose() throws Exception {
        final VirtualClientConnection peer = this.virtualConnection;
        State oldState = state;
        try {
            if (oldState != State.CLOSED) {
                // Update all internal state before the closeFuture is notified.
                if (localAddress != null) {
                    if (parent() == null) {
                        VirtualChannelRegistry.unregister(localAddress);
                    }
                    localAddress = null;
                }

                // State change must happen before finishPeerRead to ensure writes are released either in doWrite or
                // channelRead.
                state = State.CLOSED;

                ChannelPromise promise = connectPromise;
                if (promise != null) {
                    // Use tryFailure() instead of setFailure() to avoid the race against cancel().
                    promise.tryFailure(new ClosedChannelException());
                    connectPromise = null;
                }
            }

            if (peer != null) {
                peer.close();
            }
        } finally {
            // Release all buffers if the Channel was already registered in the past and if it was not closed before.
            if (oldState != null && oldState != State.CLOSED) {
                // We need to release all the buffers that may be put into our inbound queue since we closed the Channel
                // to ensure we not leak any memory. This is fine as it basically gives the same guarantees as TCP which
                // means even if the promise was notified before its not really guaranteed that the "remote peer" will
                // see the buffer at all.
                releaseInboundBuffers();
            }
        }
    }

    private void tryClose(boolean isActive) {
        if (isActive) {
            unsafe().close(unsafe().voidPromise());
        } else {
            releaseInboundBuffers();
        }
    }

    @Override
    protected void doDeregister() throws Exception {
        // Just remove the shutdownHook as this Channel may be closed later or registered to another EventLoop
        ((SingleThreadEventExecutor) eventLoop()).removeShutdownHook(shutdownHook);
    }

    protected void readInbound() {
        RecvByteBufAllocator.Handle handle = unsafe().recvBufAllocHandle();
        handle.reset(config());
        ChannelPipeline pipeline = pipeline();
        do {
            Object received = inboundBuffer.poll();
            if (received == null) {
                break;
            }
            pipeline.fireChannelRead(received);
        } while (handle.continueReading());

        pipeline.fireChannelReadComplete();
    }

    @Override
    protected void doBeginRead() throws Exception {
        if (readInProgress) {
            return;
        }

        Queue<Object> inboundBuffer = this.inboundBuffer;
        if (inboundBuffer.isEmpty()) {
            readInProgress = true;
            return;
        }

        final InternalThreadLocalMap threadLocals = InternalThreadLocalMap.get();
        final Integer stackDepth = threadLocals.localChannelReaderStackDepth();
        if (stackDepth < MAX_READER_STACK_DEPTH) {
            threadLocals.setLocalChannelReaderStackDepth(stackDepth + 1);
            try {
                readInbound();
            } finally {
                threadLocals.setLocalChannelReaderStackDepth(stackDepth);
            }
        } else {
            try {
                eventLoop().execute(readTask);
            } catch (Throwable cause) {
                logger.warn("Closing Local channels {}-{} because exception occurred!", this, cause);
                close();
                virtualConnection.close();
                PlatformDependent.throwException(cause);
            }
        }
    }

    @Override
    protected void doWrite(ChannelOutboundBuffer in) throws Exception {
        switch (state) {
            case OPEN:
            case BOUND:
                throw new NotYetConnectedException();
            case CLOSED:
                throw new ClosedChannelException();
            case CONNECTED:
                break;
        }

        final VirtualClientConnection peer = this.virtualConnection;

        writeInProgress = true;
        try {
            ClosedChannelException exception = null;
            for (;;) {
                Object msg = in.current();
                if (msg == null) {
                    break;
                }
                try {
                    // It is possible the peer could have closed while we are writing, and in this case we should
                    // simulate real socket behavior and ensure the sendMessage operation is failed.
                    if (peer.isConnected()) {
                        peer.queue().add(ReferenceCountUtil.retain(msg));
                        in.remove();
                    } else {
                        if (exception == null) {
                            exception = new ClosedChannelException();
                        }
                        in.remove(exception);
                    }
                } catch (Throwable cause) {
                    in.remove(cause);
                }
            }
        } finally {
            // The following situation may cause trouble:
            // 1. Write (with promise X)
            // 2. promise X is completed when in.remove() is called, and a listener on this promise calls close()
            // 3. Then the close event will be executed for the peer before the sendMessage events, when the sendMessage events
            // actually happened before the close event.
            writeInProgress = false;
        }
    }

    private void releaseInboundBuffers() {
        assert eventLoop() == null || eventLoop().inEventLoop();
        readInProgress = false;
        Queue<Object> inboundBuffer = this.inboundBuffer;
        Object msg;
        while ((msg = inboundBuffer.poll()) != null) {
            ReferenceCountUtil.release(msg);
        }
    }

    private class LocalUnsafe extends AbstractUnsafe {

        @Override
        public void connect(final SocketAddress remoteAddress,
                SocketAddress localAddress, final ChannelPromise promise) {
            if (!promise.setUncancellable() || !ensureOpen(promise)) {
                return;
            }

            if (state == State.CONNECTED) {
                Exception cause = new AlreadyConnectedException();
                safeSetFailure(promise, cause);
                pipeline().fireExceptionCaught(cause);
                return;
            }

            if (connectPromise != null) {
                throw new ConnectionPendingException();
            }

            connectPromise = promise;

            if (state != State.BOUND) {
                // Not bound yet and no localAddress specified - get one.
                if (localAddress == null) {
                    localAddress = new VirtualAddress(VirtualChannel.this);
                }
            }

            if (localAddress != null) {
                try {
                    doBind(localAddress);
                } catch (Throwable t) {
                    safeSetFailure(promise, t);
                    close(voidPromise());
                    return;
                }
            }

        }
    }
}
