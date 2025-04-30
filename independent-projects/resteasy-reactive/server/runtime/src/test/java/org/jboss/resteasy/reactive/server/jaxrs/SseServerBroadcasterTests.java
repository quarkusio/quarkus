package org.jboss.resteasy.reactive.server.jaxrs;

import static org.mockito.ArgumentMatchers.any;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.SseBroadcaster;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.SseUtil;
import org.jboss.resteasy.reactive.server.spi.ServerHttpResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class SseServerBroadcasterTests {

    @Test
    public void shouldCloseRegisteredSinksWhenClosingBroadcaster() {
        OutboundSseEvent.Builder builder = SseImpl.INSTANCE.newEventBuilder();
        SseBroadcaster broadcaster = SseImpl.INSTANCE.newBroadcaster();
        SseEventSinkImpl sseEventSink = Mockito.spy(new SseEventSinkImpl(getMockContext()));
        broadcaster.register(sseEventSink);
        try (MockedStatic<SseUtil> utilities = Mockito.mockStatic(SseUtil.class)) {
            utilities.when(() -> SseUtil.send(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));
            broadcaster.broadcast(builder.data("test").build());
            broadcaster.close();
            Mockito.verify(sseEventSink).close();
        }
    }

    @Test
    public void shouldNotSendToClosedSink() {
        OutboundSseEvent.Builder builder = SseImpl.INSTANCE.newEventBuilder();
        SseBroadcaster broadcaster = SseImpl.INSTANCE.newBroadcaster();
        SseEventSinkImpl sseEventSink = Mockito.spy(new SseEventSinkImpl(getMockContext()));
        broadcaster.register(sseEventSink);
        try (MockedStatic<SseUtil> utilities = Mockito.mockStatic(SseUtil.class)) {
            utilities.when(() -> SseUtil.send(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));
            OutboundSseEvent sseEvent = builder.data("test").build();
            broadcaster.broadcast(sseEvent);
            sseEventSink.close();
            broadcaster.broadcast(builder.data("should-not-be-sent").build());
            Mockito.verify(sseEventSink).send(sseEvent);
        }
    }

    @Test
    public void shouldExecuteOnClose() {
        // init broadcaster
        SseBroadcaster broadcaster = SseImpl.INSTANCE.newBroadcaster();
        AtomicBoolean executed = new AtomicBoolean(false);
        broadcaster.onClose(sink -> executed.set(true));
        // init sink
        ResteasyReactiveRequestContext mockContext = getMockContext();
        SseEventSinkImpl sseEventSink = new SseEventSinkImpl(mockContext);
        SseEventSinkImpl sinkSpy = Mockito.spy(sseEventSink);
        broadcaster.register(sinkSpy);
        try (MockedStatic<SseUtil> utilities = Mockito.mockStatic(SseUtil.class)) {
            utilities.when(() -> SseUtil.send(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));
            // call to register onCloseHandler
            ServerHttpResponse response = mockContext.serverResponse();
            sinkSpy.sendInitialResponse(response);
            ArgumentCaptor<Runnable> closeHandler = ArgumentCaptor.forClass(Runnable.class);
            Mockito.verify(response).addCloseHandler(closeHandler.capture());
            // run closeHandler to simulate closing context
            closeHandler.getValue().run();
            Assertions.assertTrue(executed.get());
        }
    }

    private ResteasyReactiveRequestContext getMockContext() {
        ResteasyReactiveRequestContext requestContext = Mockito.mock(ResteasyReactiveRequestContext.class);
        ServerHttpResponse response = Mockito.mock(ServerHttpResponse.class);
        Mockito.when(requestContext.serverResponse()).thenReturn(response);
        return requestContext;
    }
}
