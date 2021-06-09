package io.quarkus.vertx.http.deployment.devmode;

import static io.quarkus.runtime.util.HashUtil.sha256;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
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
            //the server is now up to date
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
            HttpURLConnection connection;
            log.info("Sending " + entry.getKey());
            connection = (HttpURLConnection) new URL(url + "/" + entry.getKey()).openConnection();
            connection.setRequestMethod("PUT");
            connection.setDoOutput(true);
            connection.addRequestProperty(HttpHeaders.CONTENT_TYPE.toString(), RemoteSyncHandler.APPLICATION_QUARKUS);
            connection.addRequestProperty(RemoteSyncHandler.QUARKUS_SESSION_COUNT, Integer.toString(currentSessionCounter));

            connection.addRequestProperty(RemoteSyncHandler.QUARKUS_PASSWORD,
                    sha256(sha256(entry.getValue()) + session + currentSessionCounter + password));
            currentSessionCounter++;
            connection.addRequestProperty(RemoteSyncHandler.QUARKUS_SESSION, session);
            connection.getOutputStream().write(entry.getValue());
            connection.getOutputStream().close();
            IoUtil.readBytes(connection.getInputStream());
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
            connection.addRequestProperty(HttpHeaders.CONTENT_TYPE.toString(), RemoteSyncHandler.APPLICATION_QUARKUS);
            //for the connection we use the hash of the password and the contents
            //this can be replayed, but only with the same contents, and this does not affect the server
            //state anyway
            //subsequent requests need to use the randomly generated session ID which prevents replay
            //when actually updating the server
            connection.addRequestProperty(RemoteSyncHandler.QUARKUS_PASSWORD, sha256(dataHash + password));
            connection.setDoOutput(true);

            connection.getOutputStream().write(initialData);
            connection.getOutputStream().close();
            String session = connection.getHeaderField(RemoteSyncHandler.QUARKUS_SESSION);
            String error = connection.getHeaderField(RemoteSyncHandler.QUARKUS_ERROR);
            if (error != null) {
                throw createIOException("Server did not start a remote dev session: " + error);
            }
            if (session == null) {
                throw createIOException(
                        "Server did not start a remote dev session. Make sure the environment variable 'QUARKUS_LAUNCH_DEVMODE' is set to 'true' when launching the server");
            }
            String result = new String(IoUtil.readBytes(connection.getInputStream()), StandardCharsets.UTF_8);
            Set<String> changed = new HashSet<>();
            changed.addAll(Arrays.asList(result.split(";")));
            Map<String, byte[]> data = new LinkedHashMap<>(initialConnectFunction.apply(changed));
            //this file needs to be sent last
            //if it is modified it will trigger a reload
            //and we need the rest of the app to be present
            byte[] lastFile = data.remove(QuarkusEntryPoint.LIB_DEPLOYMENT_DEPLOYMENT_CLASS_PATH_DAT);
            if (lastFile != null) {
                data.put(QuarkusEntryPoint.LIB_DEPLOYMENT_DEPLOYMENT_CLASS_PATH_DAT, lastFile);
            }

            for (Map.Entry<String, byte[]> entry : data.entrySet()) {
                sendData(entry, session);
            }
            if (lastFile != null) {
                //bit of a hack, but if we sent this the app is going to restart
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
                    connection.setRequestMethod("POST");
                    connection.addRequestProperty(HttpHeaders.CONTENT_TYPE.toString(), RemoteSyncHandler.APPLICATION_QUARKUS);
                    connection.addRequestProperty(RemoteSyncHandler.QUARKUS_SESSION_COUNT,
                            Integer.toString(currentSessionCounter));
                    connection.addRequestProperty(RemoteSyncHandler.QUARKUS_PASSWORD,
                            sha256(sha256(baos.toByteArray()) + sessionId + currentSessionCounter + password));
                    currentSessionCounter++;
                    connection.addRequestProperty(RemoteSyncHandler.QUARKUS_SESSION, sessionId);
                    connection.setDoOutput(true);
                    connection.getOutputStream().write(baos.toByteArray());

                    IoUtil.readBytes(connection.getInputStream());
                    int status = connection.getResponseCode();
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
                            connection.getOutputStream().close();
                            IoUtil.readBytes(connection.getInputStream());
                        }
                    } else if (status == 203) {
                        //need a new session
                        sessionId = doConnect(initialState, initialConnectFunction);
                    }
                    errorCount = 0;
                } catch (Throwable e) {
                    errorCount++;
                    log.error("Remote dev request failed", e);
                    if (errorCount == retryMaxAttempts) {
                        log.error("Connection failed after 10 retries, exiting");
                        return;
                    }
                    try {
                        Thread.sleep(retryIntervalMillis);
                    } catch (InterruptedException ex) {

                    }
                }
            }

        }

        private String waitForRestart(RemoteDevState initialState,
                Function<Set<String>, Map<String, byte[]>> initialConnectFunction) {

            long timeout = System.currentTimeMillis() + reconnectTimeoutMillis;
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {

            }
            while (System.currentTimeMillis() < timeout) {
                try {
                    HttpURLConnection connection = (HttpURLConnection) probeUrl.openConnection();
                    connection.setRequestMethod("POST");
                    connection.addRequestProperty(HttpHeaders.CONTENT_TYPE.toString(), RemoteSyncHandler.APPLICATION_QUARKUS);
                    IoUtil.readBytes(connection.getInputStream());
                    return doConnect(initialState, initialConnectFunction);
                } catch (IOException e) {

                }
            }
            throw new RuntimeException("Could not connect to remote side after restart");
        }

    }

}
