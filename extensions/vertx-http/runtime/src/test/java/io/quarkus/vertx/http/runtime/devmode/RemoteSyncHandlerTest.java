package io.quarkus.vertx.http.runtime.devmode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkus.dev.spi.HotReplacementContext;
import io.quarkus.dev.spi.RemoteDevState;
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
        RemoteSyncHandler.sessionState = RemoteSyncHandler.SessionState.inactive();
        RemoteSyncHandler.remoteProblem = null;
        RemoteSyncHandler.checkForChanges = false;
    }

    @Test
    void putStripsConfiguredRootPathBeforeUpdatingFile() throws Exception {
        byte[] data = "content".getBytes(StandardCharsets.UTF_8);
        var context = mock(HotReplacementContext.class);
        var handler = handler(context);

        invokeHandlePut(handler, request("/root/app/classes/com/acme/Foo.class", data));

        verify(context).updateFile("/app/classes/com/acme/Foo.class", data);
    }

    @Test
    void putLeavesPathUnchangedWhenRootPathDoesNotMatch() throws Exception {
        byte[] data = "content".getBytes(StandardCharsets.UTF_8);
        var context = mock(HotReplacementContext.class);
        var handler = handler(context);

        invokeHandlePut(handler, request("/other/app/classes/com/acme/Foo.class", data));

        verify(context).updateFile("/other/app/classes/com/acme/Foo.class", data);
    }

    @Test
    void deletePassesRawRequestPathToUpdateFile() throws Exception {
        var context = mock(HotReplacementContext.class);
        var handler = handler(context);

        invokeHandleDelete(handler, request("/root/app/classes/com/acme/Foo.class", null));

        verify(context).deleteFile("/app/classes/com/acme/Foo.class");
    }

    @Test
    void rejectedPutReturnsBadRequest() throws Exception {
        byte[] data = "content".getBytes(StandardCharsets.UTF_8);
        var context = mock(HotReplacementContext.class);
        doThrow(new IllegalArgumentException("outside root")).when(context)
                .updateFile("/app/classes/com/acme/Foo.class", data);
        var handler = handler(context);
        HttpServerRequest request = request("/root/app/classes/com/acme/Foo.class", data);

        invokeHandlePut(handler, request);

        verify(request.response()).setStatusCode(400);
        verify(request.response()).end();
    }

    @Test
    void rejectedDeleteReturnsBadRequest() throws Exception {
        var context = mock(HotReplacementContext.class);
        doThrow(new IllegalArgumentException("outside root")).when(context)
                .deleteFile("/app/classes/com/acme/Foo.class");
        var handler = handler(context);
        HttpServerRequest request = request("/root/app/classes/com/acme/Foo.class", null);

        invokeHandleDelete(handler, request);

        verify(request.response()).setStatusCode(400);
        verify(request.response()).end();
    }

    @Test
    void failedPutReturnsInternalServerError() throws Exception {
        byte[] data = "content".getBytes(StandardCharsets.UTF_8);
        var context = mock(HotReplacementContext.class);
        doThrow(new IllegalStateException("write failed")).when(context)
                .updateFile("/app/classes/com/acme/Foo.class", data);
        var handler = handler(context);
        HttpServerRequest request = request("/root/app/classes/com/acme/Foo.class", data);

        invokeHandlePut(handler, request);

        verify(request.response()).setStatusCode(500);
        verify(request.response()).end();
    }

    @Test
    void failedDeleteReturnsInternalServerError() throws Exception {
        var context = mock(HotReplacementContext.class);
        doThrow(new IllegalStateException("delete failed")).when(context)
                .deleteFile("/app/classes/com/acme/Foo.class");
        var handler = handler(context);
        HttpServerRequest request = request("/root/app/classes/com/acme/Foo.class", null);

        invokeHandleDelete(handler, request);

        verify(request.response()).setStatusCode(500);
        verify(request.response()).end();
    }

    @Test
    void invalidAuthenticatorDoesNotConsumeAHigherCounterOrRefreshTheSession() throws Exception {
        byte[] data = "content".getBytes(StandardCharsets.UTF_8);
        var context = mock(HotReplacementContext.class);
        var handler = handler(context);
        long timeout = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1);
        RemoteSyncHandler.sessionState = new RemoteSyncHandler.SessionState(SESSION, 0, timeout);
        HttpServerRequest rejected = request("/root/app/classes/com/acme/Foo.class", data,
                SESSION, 100, "incorrect");

        invokeHandlePut(handler, rejected);

        verify(rejected.response()).setStatusCode(401);
        assertThat(RemoteSyncHandler.sessionState)
                .isEqualTo(new RemoteSyncHandler.SessionState(SESSION, 0, timeout));

        HttpServerRequest accepted = request("/root/app/classes/com/acme/Foo.class", data,
                SESSION, 1, authenticator(data, SESSION, 1));
        invokeHandlePut(handler, accepted);

        verify(context).updateFile("/app/classes/com/acme/Foo.class", data);
        assertThat(RemoteSyncHandler.sessionState.acceptedCounter()).isEqualTo(1);
        assertThat(RemoteSyncHandler.sessionState.expiresAt()).isGreaterThan(timeout);
    }

    @Test
    void malformedCounterIsRejectedWithoutMutatingTheSession() throws Exception {
        byte[] data = "content".getBytes(StandardCharsets.UTF_8);
        var context = mock(HotReplacementContext.class);
        var handler = handler(context);
        long timeout = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1);
        RemoteSyncHandler.SessionState expected = new RemoteSyncHandler.SessionState(SESSION, 0, timeout);
        for (String invalidCount : new String[] { null, "", " ", "not-a-number", "2147483648", "-1", "0" }) {
            RemoteSyncHandler.sessionState = expected;
            HttpServerRequest request = request("/root/app/classes/com/acme/Foo.class", data,
                    SESSION, invalidCount, "irrelevant");

            invokeHandlePut(handler, request);

            verify(request.response()).setStatusCode(203);
            assertThat(RemoteSyncHandler.sessionState).isEqualTo(expected);
        }
        verify(context, never()).updateFile(any(), any());
    }

    @Test
    void malformedAuthenticatorsAreRejectedWithoutMutatingTheSession() throws Exception {
        byte[] data = "content".getBytes(StandardCharsets.UTF_8);
        var context = mock(HotReplacementContext.class);
        var handler = handler(context);
        long timeout = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1);
        RemoteSyncHandler.SessionState expected = new RemoteSyncHandler.SessionState(SESSION, 0, timeout);
        for (String invalidAuthenticator : new String[] { null, "", "short", "g".repeat(64) }) {
            RemoteSyncHandler.sessionState = expected;
            HttpServerRequest request = request("/root/app/classes/com/acme/Foo.class", data,
                    SESSION, "1", invalidAuthenticator);

            invokeHandlePut(handler, request);

            verify(request.response()).setStatusCode(401);
            assertThat(RemoteSyncHandler.sessionState).isEqualTo(expected);
        }
        verify(context, never()).updateFile(any(), any());
    }

    @Test
    void onlyOneConcurrentRequestCanCommitTheSameCounter() throws Exception {
        byte[] data = "content".getBytes(StandardCharsets.UTF_8);
        var context = mock(HotReplacementContext.class);
        var handler = handler(context);
        RemoteSyncHandler.sessionState = new RemoteSyncHandler.SessionState(
                SESSION, 0, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1));
        HttpServerRequest first = request("/root/app/classes/com/acme/First.class", data,
                SESSION, 1, authenticator(data, SESSION, 1));
        HttpServerRequest second = request("/root/app/classes/com/acme/Second.class", data,
                SESSION, 1, authenticator(data, SESSION, 1));
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var firstResult = executor.submit(() -> {
                start.await();
                invokeHandlePut(handler, first);
                return responseHasStatus(first, 203);
            });
            var secondResult = executor.submit(() -> {
                start.await();
                invokeHandlePut(handler, second);
                return responseHasStatus(second, 203);
            });
            start.countDown();

            assertThat(List.of(firstResult.get(10, TimeUnit.SECONDS), secondResult.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(false, true);
            assertThat(RemoteSyncHandler.sessionState.acceptedCounter()).isEqualTo(1);
            verify(context).updateFile(any(), any());
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void acceptedCountersAndTheirMutationsRemainOrdered() throws Exception {
        byte[] data = "content".getBytes(StandardCharsets.UTF_8);
        var context = mock(HotReplacementContext.class);
        CountDownLatch firstMutationEntered = new CountDownLatch(1);
        CountDownLatch releaseFirstMutation = new CountDownLatch(1);
        List<String> mutations = new CopyOnWriteArrayList<>();
        doAnswer(invocation -> {
            String path = invocation.getArgument(0);
            mutations.add(path);
            if (path.contains("First")) {
                firstMutationEntered.countDown();
                assertThat(releaseFirstMutation.await(10, TimeUnit.SECONDS)).isTrue();
            }
            return null;
        }).when(context).updateFile(any(), any());
        var handler = handler(context);
        RemoteSyncHandler.sessionState = new RemoteSyncHandler.SessionState(
                SESSION, 9, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1));
        HttpServerRequest first = request("/root/app/classes/com/acme/First.class", data,
                SESSION, 10, authenticator(data, SESSION, 10));
        HttpServerRequest second = request("/root/app/classes/com/acme/Second.class", data,
                SESSION, 11, authenticator(data, SESSION, 11));
        CountDownLatch secondFinished = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var firstResult = executor.submit(() -> {
                invokeHandlePut(handler, first);
                return null;
            });
            assertThat(firstMutationEntered.await(10, TimeUnit.SECONDS)).isTrue();
            var secondResult = executor.submit(() -> {
                try {
                    invokeHandlePut(handler, second);
                } finally {
                    secondFinished.countDown();
                }
                return null;
            });

            assertThat(secondFinished.await(200, TimeUnit.MILLISECONDS)).isFalse();
            releaseFirstMutation.countDown();
            firstResult.get(10, TimeUnit.SECONDS);
            secondResult.get(10, TimeUnit.SECONDS);

            assertThat(mutations).containsExactly(
                    "/app/classes/com/acme/First.class",
                    "/app/classes/com/acme/Second.class");
            assertThat(RemoteSyncHandler.sessionState.acceptedCounter()).isEqualTo(11);
        } finally {
            releaseFirstMutation.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void sessionReplacementWaitsForAnAcceptedOldSessionMutation() throws Exception {
        byte[] data = "content".getBytes(StandardCharsets.UTF_8);
        byte[] connectBody = serialized(new RemoteDevState(Map.of(), null));
        var context = mock(HotReplacementContext.class);
        CountDownLatch mutationEntered = new CountDownLatch(1);
        CountDownLatch releaseMutation = new CountDownLatch(1);
        CountDownLatch syncStarted = new CountDownLatch(1);
        doAnswer(invocation -> {
            mutationEntered.countDown();
            assertThat(releaseMutation.await(10, TimeUnit.SECONDS)).isTrue();
            return null;
        }).when(context).updateFile(any(), any());
        when(context.syncState(any())).thenAnswer(invocation -> {
            syncStarted.countDown();
            return Set.of();
        });
        var handler = handler(context);
        RemoteSyncHandler.sessionState = new RemoteSyncHandler.SessionState(
                SESSION, 0, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1));
        HttpServerRequest oldSessionPut = request("/root/app/classes/com/acme/Old.class", data,
                SESSION, 1, authenticator(data, SESSION, 1));
        HttpServerRequest connect = connectRequest(connectBody);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var putResult = executor.submit(() -> {
                invokeHandlePut(handler, oldSessionPut);
                return null;
            });
            assertThat(mutationEntered.await(10, TimeUnit.SECONDS)).isTrue();
            var connectResult = executor.submit(() -> {
                invoke(handler, "handleConnect", connect);
                return null;
            });

            assertThat(syncStarted.await(200, TimeUnit.MILLISECONDS)).isFalse();
            assertThat(RemoteSyncHandler.sessionState.id()).isEqualTo(SESSION);
            releaseMutation.countDown();
            putResult.get(10, TimeUnit.SECONDS);
            connectResult.get(10, TimeUnit.SECONDS);

            assertThat(RemoteSyncHandler.sessionState.id()).isNotEqualTo(SESSION);
            HttpServerRequest stalePut = request("/root/app/classes/com/acme/Stale.class", data,
                    SESSION, 2, authenticator(data, SESSION, 2));
            invokeHandlePut(handler, stalePut);
            verify(stalePut.response()).setStatusCode(203);
            verify(context).updateFile(any(), any());
        } finally {
            releaseMutation.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void failedConnectDoesNotReplaceTheUsableSession() throws Exception {
        byte[] body = serialized(new RemoteDevState(Map.of("app/application.jar", "hash"), null));
        var context = mock(HotReplacementContext.class);
        doThrow(new IllegalStateException("sync failed")).when(context).syncState(any());
        var handler = handler(context);
        long timeout = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1);
        RemoteSyncHandler.SessionState previous = new RemoteSyncHandler.SessionState(SESSION, 7, timeout);
        RemoteSyncHandler.sessionState = previous;
        HttpServerRequest request = connectRequest(body);

        invoke(handler, "handleConnect", request);

        verify(request.response()).setStatusCode(500);
        assertThat(RemoteSyncHandler.sessionState).isEqualTo(previous);
    }

    @Test
    void expiredSessionIsClearedAsOneRejectedTransition() throws Exception {
        byte[] data = "content".getBytes(StandardCharsets.UTF_8);
        var handler = handler(mock(HotReplacementContext.class));
        RemoteSyncHandler.sessionState = new RemoteSyncHandler.SessionState(SESSION, 7, 1);
        HttpServerRequest request = request("/root/app/classes/com/acme/Foo.class", data,
                SESSION, 8, authenticator(data, SESSION, 8));

        invokeHandlePut(handler, request);

        verify(request.response()).setStatusCode(203);
        assertThat(RemoteSyncHandler.sessionState).isEqualTo(RemoteSyncHandler.SessionState.inactive());
    }

    private HttpServerRequest request(String path, byte[] body) {
        RemoteSyncHandler.sessionState = new RemoteSyncHandler.SessionState(
                SESSION, 0, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1));
        byte[] passwordHashInput = body == null ? path.getBytes(StandardCharsets.UTF_8) : body;
        return request(path, body, SESSION, 1, authenticator(passwordHashInput, SESSION, 1));
    }

    private RemoteSyncHandler handler(HotReplacementContext context) {
        return new RemoteSyncHandler(PASSWORD, ignored -> {
        }, context, "/root") {
            @Override
            void executeBlocking(Callable<Void> action) {
                try {
                    action.call();
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        };
    }

    private HttpServerRequest request(String path, byte[] body, String session, int sessionCounter, String password) {
        return request(path, body, session, Integer.toString(sessionCounter), password);
    }

    private HttpServerRequest request(String path, byte[] body, String session, String sessionCounter, String password) {
        var request = mock(HttpServerRequest.class);
        var response = mock(HttpServerResponse.class);
        when(request.path()).thenReturn(path);
        when(request.response()).thenReturn(response);
        when(response.setStatusCode(400)).thenReturn(response);
        when(response.setStatusCode(401)).thenReturn(response);
        when(response.setStatusCode(203)).thenReturn(response);
        when(response.setStatusCode(500)).thenReturn(response);
        MultiMap headers = MultiMap.caseInsensitiveMultiMap()
                .add(RemoteSyncHandler.QUARKUS_SESSION, session);
        if (sessionCounter != null) {
            headers.add(RemoteSyncHandler.QUARKUS_SESSION_COUNT, sessionCounter);
        }
        if (password != null) {
            headers.add(RemoteSyncHandler.QUARKUS_PASSWORD, password);
        }
        when(request.headers()).thenReturn(headers);
        when(request.bodyHandler(any())).thenAnswer(invocation -> {
            Handler<Buffer> bodyHandler = invocation.getArgument(0);
            bodyHandler.handle(Buffer.buffer(body));
            return request;
        });
        when(request.exceptionHandler(any())).thenReturn(request);
        when(request.resume()).thenReturn(request);
        return request;
    }

    private HttpServerRequest connectRequest(byte[] body) {
        var request = mock(HttpServerRequest.class);
        var response = mock(HttpServerResponse.class);
        when(request.response()).thenReturn(response);
        when(response.setStatusCode(500)).thenReturn(response);
        when(response.headers()).thenReturn(MultiMap.caseInsensitiveMultiMap());
        when(request.headers()).thenReturn(MultiMap.caseInsensitiveMultiMap()
                .add(RemoteSyncHandler.QUARKUS_PASSWORD,
                        HashUtil.sha256(HashUtil.sha256(body) + PASSWORD)));
        when(request.bodyHandler(any())).thenAnswer(invocation -> {
            Handler<Buffer> bodyHandler = invocation.getArgument(0);
            bodyHandler.handle(Buffer.buffer(body));
            return request;
        });
        when(request.exceptionHandler(any())).thenReturn(request);
        when(request.resume()).thenReturn(request);
        return request;
    }

    private String authenticator(byte[] body, String session, int sessionCounter) {
        return HashUtil.sha256(HashUtil.sha256(body) + session + sessionCounter + PASSWORD);
    }

    private byte[] serialized(Object value) throws Exception {
        var bytes = new ByteArrayOutputStream();
        try (var output = new ObjectOutputStream(bytes)) {
            output.writeObject(value);
        }
        return bytes.toByteArray();
    }

    private void invokeHandlePut(RemoteSyncHandler handler, HttpServerRequest request) throws Exception {
        invoke(handler, "handlePut", request);
    }

    private void invokeHandleDelete(RemoteSyncHandler handler, HttpServerRequest request) throws Exception {
        invoke(handler, "handleDelete", request);
    }

    private boolean responseHasStatus(HttpServerRequest request, int status) {
        return mockingDetails(request.response()).getInvocations().stream()
                .anyMatch(invocation -> invocation.getMethod().getName().equals("setStatusCode")
                        && invocation.getArgument(0).equals(status));
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
