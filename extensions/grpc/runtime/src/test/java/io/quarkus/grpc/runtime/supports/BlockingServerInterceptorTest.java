package io.quarkus.grpc.runtime.supports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.ManagedContext;
import io.quarkus.grpc.runtime.supports.blocking.BlockingServerInterceptor;
import io.vertx.core.Vertx;

@SuppressWarnings({ "rawtypes", "unchecked" })
class BlockingServerInterceptorTest {
    public static final Context.Key<String> USERNAME = Context.key("username");

    BlockingServerInterceptor blockingServerInterceptor;
    Vertx vertx;

    @BeforeEach
    void setup() {
        vertx = Vertx.vertx();
        InjectableContext.ContextState contextState = mock(InjectableContext.ContextState.class);
        ManagedContext requestContext = mock(ManagedContext.class);
        when(requestContext.getState()).thenReturn(contextState);
        blockingServerInterceptor = new BlockingServerInterceptor(vertx, Collections.singletonList("blocking"), false) {
            @Override
            protected ManagedContext getRequestContext() {
                return requestContext;
            }
        };
    }

    @Test
    @Timeout(10)
    void testContextPropagation() throws Exception {
        final ServerCall serverCall = mock(ServerCall.class);
        final BlockingServerCallHandler serverCallHandler = new BlockingServerCallHandler();
        final MethodDescriptor methodDescriptor = mock(MethodDescriptor.class);
        when(methodDescriptor.getFullMethodName()).thenReturn("my-service/blocking");
        when(serverCall.getMethodDescriptor()).thenReturn(methodDescriptor);

        // setting grpc context
        final Context context = Context.current().withValue(USERNAME, "my-user");

        final ServerCall.Listener listener = blockingServerInterceptor.interceptCall(serverCall, null, serverCallHandler);
        serverCallHandler.awaitSetup();

        // simulate GRPC call
        context.wrap(() -> listener.onMessage("hello")).run();

        // await for the message to be received
        serverCallHandler.await();

        // check that the thread is a worker thread
        assertThat(serverCallHandler.threadName).contains("vert.x").contains("worker");

        // check that the context was propagated correctly
        assertThat(serverCallHandler.contextUserName).isEqualTo("my-user");
    }

    static class BlockingServerCallHandler implements ServerCallHandler {
        String threadName;
        String contextUserName;
        private final CountDownLatch latch = new CountDownLatch(1);
        private final CountDownLatch setupLatch = new CountDownLatch(1);

        @Override
        public ServerCall.Listener startCall(ServerCall serverCall, Metadata metadata) {
            final ServerCall.Listener listener = new ServerCall.Listener() {
                @Override
                public void onMessage(Object message) {
                    threadName = Thread.currentThread().getName();
                    contextUserName = USERNAME.get();
                    super.onMessage(message);
                    latch.countDown();
                }
            };
            setupLatch.countDown();
            return listener;
        }

        public void awaitSetup() throws InterruptedException {
            setupLatch.await();
            Thread.sleep(100);
        }

        public void await() throws InterruptedException {
            latch.await();
        }
    }

}
