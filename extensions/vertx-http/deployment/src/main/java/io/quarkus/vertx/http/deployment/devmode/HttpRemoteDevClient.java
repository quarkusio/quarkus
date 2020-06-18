package io.quarkus.vertx.http.deployment.devmode;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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

    public HttpRemoteDevClient(String url, String password) {
        this.url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        this.password = password;
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

    private String doConnect(RemoteDevState initialState, Function<Set<String>, Map<String, byte[]>> initialConnectFunction)
            throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url + RemoteSyncHandler.CONNECT).openConnection();
        connection.addRequestProperty(HttpHeaders.CONTENT_TYPE.toString(), RemoteSyncHandler.APPLICATION_QUARKUS);
        connection.addRequestProperty(RemoteSyncHandler.QUARKUS_PASSWORD, password);
        connection.setDoOutput(true);
        ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
        out.writeObject(initialState);
        out.close();
        String session = connection.getHeaderField(RemoteSyncHandler.QUARKUS_SESSION);
        if (session == null) {
            throw new IOException("Server did not start a remote dev session");
        }
        String result = new String(IoUtil.readBytes(connection.getInputStream()), StandardCharsets.UTF_8);
        Set<String> changed = new HashSet<>();
        changed.addAll(Arrays.asList(result.split(";")));
        Map<String, byte[]> data = new LinkedHashMap<>(initialConnectFunction.apply(changed));
        //this file needs to be sent last
        //if it is modified it will trigger a reload
        //and we need the rest of the app to be present
        byte[] lastFile = data.remove(QuarkusEntryPoint.QUARKUS_APPLICATION_DAT);
        if (lastFile != null) {
            data.put(QuarkusEntryPoint.QUARKUS_APPLICATION_DAT, lastFile);
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

    private String waitForRestart(RemoteDevState initialState,
            Function<Set<String>, Map<String, byte[]>> initialConnectFunction) {

        long timeout = System.currentTimeMillis() + 30000;
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {

        }
        while (System.currentTimeMillis() < timeout) {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                IoUtil.readBytes(connection.getInputStream());
                return doConnect(initialState, initialConnectFunction);
            } catch (IOException e) {

            }
        }
        throw new RuntimeException("Could not connect to remote side after restart");
    }

    private void sendData(Map.Entry<String, byte[]> entry, String session) throws IOException {
        HttpURLConnection connection;
        log.info("Sending " + entry.getKey());
        connection = (HttpURLConnection) new URL(url + "/" + entry.getKey()).openConnection();
        connection.setRequestMethod("PUT");
        connection.setDoOutput(true);
        connection.addRequestProperty(HttpHeaders.CONTENT_TYPE.toString(), RemoteSyncHandler.APPLICATION_QUARKUS);
        connection.addRequestProperty(RemoteSyncHandler.QUARKUS_PASSWORD, password);
        connection.addRequestProperty(RemoteSyncHandler.QUARKUS_SESSION, session);
        connection.getOutputStream().write(entry.getValue());
        connection.getOutputStream().close();
        IoUtil.readBytes(connection.getInputStream());
    }

    private class Session implements Closeable, Runnable {

        private String sessionId = null;
        private final RemoteDevState initialState;
        private final Function<Set<String>, Map<String, byte[]>> initialConnectFunction;
        private final Supplier<SyncResult> changeRequestFunction;
        private volatile boolean closed;
        private final Thread httpThread;
        private final URL url;
        int errorCount;

        private Session(RemoteDevState initialState,
                Function<Set<String>, Map<String, byte[]>> initialConnectFunction, Supplier<SyncResult> changeRequestFunction)
                throws MalformedURLException {
            this.initialState = initialState;
            this.initialConnectFunction = initialConnectFunction;
            this.changeRequestFunction = changeRequestFunction;
            url = new URL(HttpRemoteDevClient.this.url + RemoteSyncHandler.DEV);
            httpThread = new Thread(this, "Remote dev client thread");
            httpThread.start();
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
                    //long polling request
                    //we always send the current problem state
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.addRequestProperty(HttpHeaders.CONTENT_TYPE.toString(), RemoteSyncHandler.APPLICATION_QUARKUS);
                    connection.addRequestProperty(RemoteSyncHandler.QUARKUS_PASSWORD, password);
                    connection.addRequestProperty(RemoteSyncHandler.QUARKUS_SESSION, sessionId);
                    connection.setDoOutput(true);
                    try (ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream())) {
                        out.writeObject(problem);
                    }
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
                            log.info("deleting " + file);
                            connection = (HttpURLConnection) new URL(url + "/" + file).openConnection();
                            connection.setRequestMethod("DELETE");
                            connection.addRequestProperty(HttpHeaders.CONTENT_TYPE.toString(),
                                    RemoteSyncHandler.APPLICATION_QUARKUS);
                            connection.addRequestProperty(RemoteSyncHandler.QUARKUS_PASSWORD, password);
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
                    if (errorCount == 10) {
                        log.error("Connection failed after 10 retries, exiting");
                        return;
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ex) {

                    }
                }
            }

        }
    }

}
