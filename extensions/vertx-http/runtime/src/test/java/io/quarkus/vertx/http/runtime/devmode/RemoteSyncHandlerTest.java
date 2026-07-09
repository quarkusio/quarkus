package io.quarkus.vertx.http.runtime.devmode;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkus.dev.spi.HotReplacementContext;
import io.quarkus.runtime.util.HashUtil;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

class RemoteSyncHandlerTest {

    private static final String PASSWORD = "secret";
    private static final String SESSION = "session";

    @AfterEach
    void resetRemoteSyncState() {
        RemoteSyncHandler.currentSession = null;
        RemoteSyncHandler.currentSessionCounter = 0;
        RemoteSyncHandler.currentSessionTimeout = 0;
        RemoteSyncHandler.remoteProblem = null;
        RemoteSyncHandler.checkForChanges = false;
    }

    @Test
    void putStripsConfiguredRootPathBeforeUpdatingFile() throws Exception {
        byte[] data = "content".getBytes(StandardCharsets.UTF_8);
        var context = mock(HotReplacementContext.class);
        var handler = new RemoteSyncHandler(PASSWORD, ignored -> {
        }, context, "/root");

        invokeHandlePut(handler, request("/root/app/classes/com/acme/Foo.class", data));

        verify(context).updateFile("/app/classes/com/acme/Foo.class", data);
    }

    @Test
    void putLeavesPathUnchangedWhenRootPathDoesNotMatch() throws Exception {
        byte[] data = "content".getBytes(StandardCharsets.UTF_8);
        var context = mock(HotReplacementContext.class);
        var handler = new RemoteSyncHandler(PASSWORD, ignored -> {
        }, context, "/root");

        invokeHandlePut(handler, request("/other/app/classes/com/acme/Foo.class", data));

        verify(context).updateFile("/other/app/classes/com/acme/Foo.class", data);
    }

    @Test
    void deletePassesRawRequestPathToUpdateFile() throws Exception {
        var context = mock(HotReplacementContext.class);
        var handler = new RemoteSyncHandler(PASSWORD, ignored -> {
        }, context, "/root");

        invokeHandleDelete(handler, request("/root/app/classes/com/acme/Foo.class", null));

        verify(context).deleteFile("/app/classes/com/acme/Foo.class");
    }

    private HttpServerRequest request(String path, byte[] body) {
        RemoteSyncHandler.currentSession = SESSION;
        RemoteSyncHandler.currentSessionCounter = 0;
        var request = mock(HttpServerRequest.class);
        var response = mock(HttpServerResponse.class);
        when(request.path()).thenReturn(path);
        when(request.response()).thenReturn(response);
        when(request.headers()).thenReturn(headers(path, body));
        when(request.bodyHandler(any())).thenAnswer(invocation -> {
            Handler<Buffer> bodyHandler = invocation.getArgument(0);
            bodyHandler.handle(Buffer.buffer(body));
            return request;
        });
        when(request.exceptionHandler(any())).thenReturn(request);
        when(request.resume()).thenReturn(request);
        return request;
    }

    private MultiMap headers(String path, byte[] body) {
        var sessionCounter = 1;
        var passwordHashInput = body == null ? path.getBytes(StandardCharsets.UTF_8) : body;
        return MultiMap.caseInsensitiveMultiMap()
                .add(RemoteSyncHandler.QUARKUS_SESSION, SESSION)
                .add(RemoteSyncHandler.QUARKUS_SESSION_COUNT, String.valueOf(sessionCounter))
                .add(RemoteSyncHandler.QUARKUS_PASSWORD,
                        HashUtil.sha256(HashUtil.sha256(passwordHashInput) + SESSION + sessionCounter + PASSWORD));
    }

    private void invokeHandlePut(RemoteSyncHandler handler, HttpServerRequest request) throws Exception {
        invoke(handler, "handlePut", request);
    }

    private void invokeHandleDelete(RemoteSyncHandler handler, HttpServerRequest request) throws Exception {
        invoke(handler, "handleDelete", request);
    }

    private void invoke(RemoteSyncHandler handler, String methodName, HttpServerRequest request) throws Exception {
        Method method = RemoteSyncHandler.class.getDeclaredMethod(methodName, HttpServerRequest.class);
        method.setAccessible(true);
        try {
            method.invoke(handler, request);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception exception) {
                throw exception;
            }
            throw e;
        }
    }
}
