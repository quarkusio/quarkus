package io.quarkus.grpc.runtime.stork;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.smallrye.stork.api.ServiceInstance;

@SuppressWarnings({ "rawtypes", "unchecked" })
class StorkMeasuringInterceptorContextTest {

    @Test
    void shouldAttachStorkContextOnlyDuringNewCall() {
        VertxStorkMeasuringGrpcInterceptor interceptor = new VertxStorkMeasuringGrpcInterceptor();
        ClientCall delegate = mock(ClientCall.class);
        Channel channel = mock(Channel.class);
        MethodDescriptor method = unaryMethod();
        when(channel.newCall(method, CallOptions.DEFAULT)).thenAnswer(invocation -> {
            assertThat(StorkMeasuringCollector.STORK_MEASURE_TIME.get()).isNotNull();
            assertThat(StorkMeasuringCollector.STORK_SERVICE_INSTANCE.get()).isNotNull();
            return delegate;
        });
        doAnswer(invocation -> {
            assertThat(StorkMeasuringCollector.STORK_MEASURE_TIME.get()).isNull();
            assertThat(StorkMeasuringCollector.STORK_SERVICE_INSTANCE.get()).isNull();
            ClientCall.Listener listener = invocation.getArgument(0);
            listener.onClose(Status.OK, new Metadata());
            return null;
        }).when(delegate).start(any(ClientCall.Listener.class), any(Metadata.class));

        ClientCall call = interceptor.interceptCall(method, CallOptions.DEFAULT, channel);
        assertThat(StorkMeasuringCollector.STORK_MEASURE_TIME.get()).isNull();
        assertThat(StorkMeasuringCollector.STORK_SERVICE_INSTANCE.get()).isNull();

        call.start(mock(ClientCall.Listener.class), new Metadata());

        assertThat(StorkMeasuringCollector.STORK_MEASURE_TIME.get()).isNull();
        assertThat(StorkMeasuringCollector.STORK_SERVICE_INSTANCE.get()).isNull();
    }

    @Test
    void shouldDetachStorkContextWhenNewCallFails() {
        VertxStorkMeasuringGrpcInterceptor interceptor = new VertxStorkMeasuringGrpcInterceptor();
        Channel channel = mock(Channel.class);
        MethodDescriptor method = unaryMethod();
        when(channel.newCall(method, CallOptions.DEFAULT))
                .thenThrow(new IllegalStateException("newCall failed"));

        assertThat(StorkMeasuringCollector.STORK_MEASURE_TIME.get()).isNull();
        assertThat(StorkMeasuringCollector.STORK_SERVICE_INSTANCE.get()).isNull();

        assertThatThrownBy(() -> interceptor.interceptCall(method, CallOptions.DEFAULT, channel))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("newCall failed");

        assertThat(StorkMeasuringCollector.STORK_MEASURE_TIME.get()).isNull();
        assertThat(StorkMeasuringCollector.STORK_SERVICE_INSTANCE.get()).isNull();
    }

    @Test
    void shouldRecordEndWhenCallClosesSuccessfully() {
        VertxStorkMeasuringGrpcInterceptor interceptor = new VertxStorkMeasuringGrpcInterceptor();
        ClientCall delegate = mock(ClientCall.class);
        Channel channel = mock(Channel.class);
        MethodDescriptor method = unaryMethod();
        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        when(channel.newCall(method, CallOptions.DEFAULT)).thenAnswer(invocation -> {
            StorkMeasuringCollector.STORK_SERVICE_INSTANCE.get().set(serviceInstance);
            return delegate;
        });
        doAnswer(invocation -> {
            ClientCall.Listener listener = invocation.getArgument(0);
            listener.onClose(Status.OK, new Metadata());
            return null;
        }).when(delegate).start(any(ClientCall.Listener.class), any(Metadata.class));

        ClientCall call = interceptor.interceptCall(method, CallOptions.DEFAULT, channel);
        call.start(mock(ClientCall.Listener.class), new Metadata());

        verify(serviceInstance).recordEnd(isNull());
    }

    @Test
    void shouldRecordEndWithErrorWhenCallClosesUnsuccessfully() {
        VertxStorkMeasuringGrpcInterceptor interceptor = new VertxStorkMeasuringGrpcInterceptor();
        ClientCall delegate = mock(ClientCall.class);
        Channel channel = mock(Channel.class);
        MethodDescriptor method = unaryMethod();
        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        when(channel.newCall(method, CallOptions.DEFAULT)).thenAnswer(invocation -> {
            StorkMeasuringCollector.STORK_SERVICE_INSTANCE.get().set(serviceInstance);
            return delegate;
        });
        doAnswer(invocation -> {
            ClientCall.Listener listener = invocation.getArgument(0);
            listener.onClose(Status.UNAVAILABLE.withDescription("backend down"), new Metadata());
            return null;
        }).when(delegate).start(any(ClientCall.Listener.class), any(Metadata.class));

        ClientCall call = interceptor.interceptCall(method, CallOptions.DEFAULT, channel);
        call.start(mock(ClientCall.Listener.class), new Metadata());

        verify(serviceInstance).recordEnd(any(Exception.class));
    }

    @Test
    void shouldForwardOnMessageEvenIfRecordReplyFails() {
        VertxStorkMeasuringGrpcInterceptor interceptor = new VertxStorkMeasuringGrpcInterceptor();
        ClientCall delegate = mock(ClientCall.class);
        Channel channel = mock(Channel.class);
        MethodDescriptor method = unaryMethod();
        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        ClientCall.Listener responseListener = mock(ClientCall.Listener.class);
        Object message = new Object();
        when(channel.newCall(method, CallOptions.DEFAULT)).thenAnswer(invocation -> {
            StorkMeasuringCollector.STORK_SERVICE_INSTANCE.get().set(serviceInstance);
            return delegate;
        });
        doThrow(new IllegalStateException("recordReply failed")).when(serviceInstance).recordReply();
        doAnswer(invocation -> {
            ClientCall.Listener listener = invocation.getArgument(0);
            assertThatThrownBy(() -> listener.onMessage(message))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("recordReply failed");
            return null;
        }).when(delegate).start(any(ClientCall.Listener.class), any(Metadata.class));

        ClientCall call = interceptor.interceptCall(method, CallOptions.DEFAULT, channel);
        call.start(responseListener, new Metadata());

        verify(serviceInstance).recordReply();
        verify(responseListener).onMessage(message);
    }

    private static MethodDescriptor unaryMethod() {
        MethodDescriptor method = mock(MethodDescriptor.class);
        when(method.getType()).thenReturn(MethodDescriptor.MethodType.UNARY);
        return method;
    }
}
