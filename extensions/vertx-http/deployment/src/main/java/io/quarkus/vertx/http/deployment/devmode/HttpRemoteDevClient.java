package io.quarkus.vertx.http.deployment.devmode;

import static io.quarkus.runtime.util.HashUtil.sha256;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.runner.QuarkusEntryPoint;
import io.quarkus.deployment.dev.remote.RemoteDevClient;
import io.quarkus.deployment.util.IoUtil;
import io.quarkus.dev.spi.RemoteDevState;
import io.quarkus.vertx.http.runtime.devmode.RemoteSyncHandler;
import io.vertx.core.http.HttpHeaders;

public class HttpRemoteDevClient implements RemoteDevClient {

    private final Logger log = Logger.getLogger(HttpRemoteDevClient.class);

    /**
     * The default Accept header defined in sun.net.www.protocol.http.HttpURLConnection is invalid and
     * does not respect the RFC, so we override it with a valid value.
     * RESTEasy is quite strict regarding the RFC and throws an error.
     * Note that this is just the default HttpURLConnection header value made valid.
     * See https://bugs.openjdk.java.net/browse/JDK-8163921 and https://bugs.openjdk.java.net/browse/JDK-8177439
     * and https://github.com/quarkusio/quarkus/issues/20904
     */
    private static final String DEFAULT_ACCEPT = "text/html, image/gif, image/jpeg; q=0.2, */*; q=0.2";

    private final String url;
    private final String password;
    private final long reconnectTimeoutMillis;
    private final long retryIntervalMillis;
    private final int retryMaxAttempts;

    public HttpRemoteDevClient(String url, String password, Duration reconnectTimeout, Duration retryInterval,
            int retryMaxAttempts) {
        this.url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        this.password = password;
        this.reconnectTimeoutMillis = reconnectTimeout.toMillis();
        this.retryIntervalMillis = retryInterval.toMillis();
        this.retryMaxAttempts = retryMaxAttempts;
    }

    @Override
    public Closeable sendConnectRequest(RemoteDevState initialState,
            Function<Set<String>, Map<String, byte[]>> initialConnectFunction, Supplier<SyncResult> changeRequestFunction) {
        //so when we connect we send the current state
        //the server will respond with a list of files it needs, one per line as a standard UTF-8 document
        try {
            //we are now good to go
            //the server is now up-to-date
            return new Session(initialState, initialConnectFunction, changeRequestFunction);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private class Session implements Closeable, Runnable {

        private String sessionId = null;
        private int currentSessionCounter = 1;
        private final RemoteDevState initialState;
        private final Function<Set<String>, Map<String, byte[]>> initialConnectFunction;
        private final Supplier<SyncResult> changeRequestFunction;
        private volatile boolean closed;
        private final Thread httpThread;
        private final String url;
        private final URL devUrl;
        private final URL probeUrl;
        int errorCount;

        private Session(RemoteDevState initialState,
                Function<Set<String>, Map<String, byte[]>> initialConnectFunction, Supplier<SyncResult> changeRequestFunction)
                throws MalformedURLException {
            this.initialState = initialState;
            this.initialConnectFunction = initialConnectFunction;
            this.changeRequestFunction = changeRequestFunction;
            devUrl = new URL(HttpRemoteDevClient.this.url + RemoteSyncHandler.DEV);
            probeUrl = new URL(HttpRemoteDevClient.this.url + RemoteSyncHandler.PROBE);
            url = HttpRemoteDevClient.this.url;
            httpThread = new Thread(this, "Remote dev client thread");
            httpThread.start();
        }

        private void sendData(Map.Entry<String, byte[]> entry, String session) throws IOException {
            log.info("Sending " + entry.getKey());
            HttpURLConnection connection = (HttpURLConnection) new URL(url + "/" + entry.getKey()).openConnection();
            connection.setRequestMethod("PUT");
            connection.setDoOutput(true);
            connection.setRequestProperty(HttpHeaders.ACCEPT.toString(), DEFAULT_ACCEPT);
            connection.addRequestProperty(HttpHeaders.CONTENT_TYPE.toString(), RemoteSyncHandler.APPLICATION_QUARKUS);
            connection.addRequestProperty(RemoteSyncHandler.QUARKUS_SESSION_COUNT, Integer.toString(currentSessionCounter));

            connection.addRequestProperty(RemoteSyncHandler.QUARKUS_PASSWORD,
                    sha256(sha256(entry.getValue()) + session + currentSessionCounter + password));
            currentSessionCounter++;
            connection.addRequestProperty(RemoteSyncHandler.QUARKUS_SESSION, session);
            exchange(connection, entry.getValue(), "send a remote file");
        }

        private String doConnect(RemoteDevState initialState, Function<Set<String>, Map<String, byte[]>> initialConnectFunction)
                throws IOException {

            currentSessionCounter = 1;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(baos)) {
                out.writeObject(initialState);
            }
            byte[] initialData = baos.toByteArray();
            String dataHash = sha256(initialData);

            HttpURLConnection connection = (HttpURLConnection) new URL(url + RemoteSyncHandler.CONNECT)
                    .openConnection();
            connection.setRequestProperty(HttpHeaders.ACCEPT.toString(), DEFAULT_ACCEPT);
            connection.addRequestProperty(HttpHeaders.CONTENT_TYPE.toString(), RemoteSyncHandler.APPLICATION_QUARKUS);
            //for the connection we use the hash of the password and the contents
            //this can be replayed, but only with the same contents, and this does not affect the server
            //state anyway
            //subsequent requests need to use the randomly generated session ID which prevents replay
            //when actually updating the server
            connection.addRequestProperty(RemoteSyncHandler.QUARKUS_PASSWORD, sha256(dataHash + password));
            connection.setDoOutput(true);

            Response response = exchange(connection, initialData, "start a remote dev session");
            String session = response.session();
            if (session == null) {
                throw createIOException(
                        "Server did not start a remote dev session. Make sure the environment variable 'QUARKUS_LAUNCH_DEVMODE' is set to 'true' when launching the server");
            }
            String result = new String(response.body(), StandardCharsets.UTF_8);
            Set<String> changed = new HashSet<>();
            changed.addAll(Arrays.asList(result.split(";")));
            Map<String, byte[]> data = new LinkedHashMap<>(initialConnectFunction.apply(changed));
            //this file needs to be sent last
            //if it is modified it will trigger a reload
            //and we need the rest of the app to be present
            byte[] lastFile = data.remove(QuarkusEntryPoint.LIB_DEPLOYMENT_APPMODEL_DAT);
            if (lastFile != null) {
                data.put(QuarkusEntryPoint.LIB_DEPLOYMENT_APPMODEL_DAT, lastFile);
            }

            for (Map.Entry<String, byte[]> entry : data.entrySet()) {
                sendData(entry, session);
            }
            if (lastFile != null) {
                //a bit of a hack, but if we sent this the app is going to restart
                //if we attempt to connect too soon it won't be ready
                session = waitForRestart(initialState, initialConnectFunction);
            } else {
                log.info("Connected to remote server");
            }
            return session;
        }

        private IOException createIOException(String message) {
            IOException result = new IOException(message);
            result.setStackTrace(new StackTraceElement[] {});
            return result;
        }

        @Override
        public void close() throws IOException {
            closed = true;
            httpThread.interrupt();
        }

        @Override
        public void run() {
            Throwable problem = null;
            while (!closed) {

                HttpURLConnection connection = null;
                try {
                    if (sessionId == null) {
                        sessionId = doConnect(initialState, initialConnectFunction);
                    }

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    try (ObjectOutputStream out = new ObjectOutputStream(baos)) {
                        out.writeObject(problem);
                    }
                    //long polling request
                    //we always send the current problem state
                    connection = (HttpURLConnection) devUrl.openConnection();
                    connection.setRequestProperty(HttpHeaders.ACCEPT.toString(), DEFAULT_ACCEPT);
                    connection.setRequestMethod("POST");
                    connection.addRequestProperty(HttpHeaders.CONTENT_TYPE.toString(), RemoteSyncHandler.APPLICATION_QUARKUS);
                    connection.addRequestProperty(RemoteSyncHandler.QUARKUS_SESSION_COUNT,
                            Integer.toString(currentSessionCounter));
                    connection.addRequestProperty(RemoteSyncHandler.QUARKUS_PASSWORD,
                            sha256(sha256(baos.toByteArray()) + sessionId + currentSessionCounter + password));
                    currentSessionCounter++;
                    connection.addRequestProperty(RemoteSyncHandler.QUARKUS_SESSION, sessionId);
                    connection.setDoOutput(true);
                    Response response = exchange(connection, baos.toByteArray(), "poll for remote changes");
                    int status = response.status();
                    if (status == 200) {
                        SyncResult sync = changeRequestFunction.get();
                        problem = sync.getProblem();
                        //if there have been any changes send the new files
                        for (Map.Entry<String, byte[]> entry : sync.getChangedFiles().entrySet()) {
                            sendData(entry, sessionId);
                        }
                        for (String file : sync.getRemovedFiles()) {
                            if (file.endsWith("META-INF/MANIFEST.MF") || file.contains("META-INF/maven")
                                    || !file.contains("/")) {
                                //we have some filters, for files that we don't want to delete
                                continue;
                            }
                            log.info("deleting " + file);
                            connection = (HttpURLConnection) new URL(url + "/" + file).openConnection();
                            connection.setRequestProperty(HttpHeaders.ACCEPT.toString(), DEFAULT_ACCEPT);
                            connection.setRequestMethod("DELETE");
                            connection.addRequestProperty(HttpHeaders.CONTENT_TYPE.toString(),
                                    RemoteSyncHandler.APPLICATION_QUARKUS);
                            connection.addRequestProperty(RemoteSyncHandler.QUARKUS_SESSION_COUNT,
                                    Integer.toString(currentSessionCounter));
                            //for delete requests we add the path to the password hash
                            connection.addRequestProperty(RemoteSyncHandler.QUARKUS_PASSWORD,
                                    sha256(sha256("/" + file) + sessionId + currentSessionCounter + password));
                            currentSessionCounter++;
                            connection.addRequestProperty(RemoteSyncHandler.QUARKUS_SESSION, sessionId);
                            exchange(connection, null, "delete a remote file");
                        }
                    } else if (status == 203) {
                        //need a new session
                        sessionId = doConnect(initialState, initialConnectFunction);
                    }
                    errorCount = 0;
                } catch (RemoteDevResponseException e) {
                    if (e.permanent()) {
                        log.error("Remote dev request cannot be completed", e);
                        return;
                    }
                    errorCount++;
                    log.error("Remote dev request failed", e);
                    if (errorCount == retryMaxAttempts) {
                        log.errorf("Connection failed after %d retries, exiting", errorCount);
                        return;
                    }
                    sleepBeforeRetry(e.retryAfterMillis());
                } catch (Throwable e) {
                    errorCount++;
                    log.error("Remote dev request failed", e);
                    if (errorCount == retryMaxAttempts) {
                        log.errorf("Connection failed after %d retries, exiting", errorCount);
                        return;
                    }
                    sleepBeforeRetry(retryIntervalMillis);
                }
            }

        }

        private String waitForRestart(RemoteDevState initialState,
                Function<Set<String>, Map<String, byte[]>> initialConnectFunction) throws IOException {

            long timeout = System.currentTimeMillis() + reconnectTimeoutMillis;
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {

            }
            while (!closed && System.currentTimeMillis() < timeout) {
                try {
                    HttpURLConnection connection = (HttpURLConnection) probeUrl.openConnection();
                    connection.setRequestProperty(HttpHeaders.ACCEPT.toString(), DEFAULT_ACCEPT);
                    connection.setRequestMethod("POST");
                    connection.addRequestProperty(HttpHeaders.CONTENT_TYPE.toString(), RemoteSyncHandler.APPLICATION_QUARKUS);
                    exchange(connection, null, "probe the remote server");
                    return doConnect(initialState, initialConnectFunction);
                } catch (RemoteDevResponseException e) {
                    if (e.permanent()) {
                        throw e;
                    }
                    sleepBeforeRetry(e.retryAfterMillis());
                } catch (IOException e) {
                    sleepBeforeRetry(retryIntervalMillis);
                }
            }
            throw createIOException(closed
                    ? "Remote dev session closed while waiting for the remote side to restart"
                    : "Could not connect to remote side after restart");
        }

        private Response exchange(HttpURLConnection connection, byte[] body, String operation) throws IOException {
            try {
                if (body != null) {
                    connection.setFixedLengthStreamingMode(body.length);
                    try (var output = connection.getOutputStream()) {
                        output.write(body);
                    }
                }
                int status = connection.getResponseCode();
                byte[] responseBody;
                InputStream input = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
                if (input == null) {
                    responseBody = new byte[0];
                } else {
                    try (input) {
                        responseBody = IoUtil.readBytes(input);
                    }
                }
                String error = connection.getHeaderField(RemoteSyncHandler.QUARKUS_ERROR);
                if (status >= 400) {
                    String detail = error == null || error.isBlank() ? "HTTP " + status : error;
                    boolean permanent = status == 413 || status == 415;
                    long retryAfter = status == 503 ? retryAfterMillis(connection) : retryIntervalMillis;
                    throw new RemoteDevResponseException(
                            "Server could not " + operation + ": " + detail, permanent, retryAfter);
                }
                return new Response(status, responseBody,
                        connection.getHeaderField(RemoteSyncHandler.QUARKUS_SESSION));
            } finally {
                connection.disconnect();
            }
        }

        private long retryAfterMillis(HttpURLConnection connection) {
            String value = connection.getHeaderField("Retry-After");
            if (value == null) {
                return retryIntervalMillis;
            }
            try {
                long millis = Math.multiplyExact(Long.parseLong(value), 1000);
                long maximum = reconnectTimeoutMillis > 0 ? reconnectTimeoutMillis : retryIntervalMillis;
                return Math.min(Math.max(millis, retryIntervalMillis), maximum);
            } catch (ArithmeticException | NumberFormatException ignored) {
                return retryIntervalMillis;
            }
        }

        private void sleepBeforeRetry(long delay) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }

        private record Response(int status, byte[] body, String session) {
        }

        private final class RemoteDevResponseException extends IOException {

            private final boolean permanent;
            private final long retryAfterMillis;

            private RemoteDevResponseException(String message, boolean permanent, long retryAfterMillis) {
                super(message);
                this.permanent = permanent;
                this.retryAfterMillis = retryAfterMillis;
                setStackTrace(new StackTraceElement[] {});
            }

            private boolean permanent() {
                return permanent;
            }

            private long retryAfterMillis() {
                return retryAfterMillis;
            }
        }

    }

}
