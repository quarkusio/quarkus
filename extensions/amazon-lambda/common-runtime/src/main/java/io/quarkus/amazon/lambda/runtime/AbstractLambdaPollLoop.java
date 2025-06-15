package io.quarkus.amazon.lambda.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import io.quarkus.runtime.Application;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.ShutdownContext;

public abstract class AbstractLambdaPollLoop {
    private static final Logger log = Logger.getLogger(AbstractLambdaPollLoop.class);

    private static final String USER_AGENT = "User-Agent";
    private static final String USER_AGENT_VALUE = String.format("quarkus/%s-%s",
            System.getProperty("java.vendor.version"),
            AbstractLambdaPollLoop.class.getPackage().getImplementationVersion());

    private final ObjectMapper objectMapper;
    private final ObjectReader cognitoIdReader;
    private final ObjectReader clientCtxReader;
    private final LaunchMode launchMode;
    private static final String LAMBDA_TRACE_HEADER_PROP = "com.amazonaws.xray.traceHeader";
    private static final String MDC_AWS_REQUEST_ID_KEY = "AWSRequestId";

    public AbstractLambdaPollLoop(ObjectMapper objectMapper, ObjectReader cognitoIdReader, ObjectReader clientCtxReader,
            LaunchMode launchMode) {
        this.objectMapper = objectMapper;
        this.cognitoIdReader = cognitoIdReader;
        this.clientCtxReader = clientCtxReader;
        this.launchMode = launchMode;
    }

    protected boolean shouldLog(Exception e) {
        return true;
    }

    protected abstract boolean isStream();

    protected HttpURLConnection requestConnection = null;

    public void startPollLoop(ShutdownContext context) {
        final AtomicBoolean running = new AtomicBoolean(true);
        // flag to check whether to interrupt.
        final AtomicBoolean shouldInterrupt = new AtomicBoolean(true);
        String baseUrl = AmazonLambdaApi.baseUrl();
        final Thread pollingThread = new Thread(new Runnable() {
            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                try {
                    if (!LambdaHotReplacementRecorder.enabled
                            && (launchMode == LaunchMode.DEVELOPMENT || launchMode == LaunchMode.NORMAL)) {
                        // when running with continuous testing, this method fails
                        // because currentApplication is not set when running as an
                        // auxiliary application. So, just skip it if hot replacement enabled.
                        // This method is called to determine if Quarkus is started and ready to receive requests.
                        checkQuarkusBootstrapped();
                    }
                    URL requestUrl = AmazonLambdaApi.invocationNext(baseUrl);
                    if (AmazonLambdaApi.isTestMode()) {
                        // FYI: This log is required as native test runner
                        // looks for "Listening on" in log to ensure native executable booted
                        log.info("Listening on: " + requestUrl.toString());
                    }
                    while (running.get()) {

                        try {
                            requestConnection = (HttpURLConnection) requestUrl.openConnection();
                            requestConnection.setRequestProperty(USER_AGENT, USER_AGENT_VALUE);
                        } catch (IOException e) {
                            if (!running.get()) {
                                // just return gracefully as we were probably shut down by
                                // shutdown task
                                return;
                            }
                            if (abortGracefully(e)) {
                                return;
                            }
                            throw e;
                        }
                        try {
                            String requestId = requestConnection
                                    .getHeaderField(AmazonLambdaApi.LAMBDA_RUNTIME_AWS_REQUEST_ID);
                            if (requestId != null) {
                                MDC.put(MDC_AWS_REQUEST_ID_KEY, requestId);
                            }
                            if (requestConnection.getResponseCode() != 200) {
                                // connection should be closed by finally clause
                                continue;
                            }
                            try {
                                if (LambdaHotReplacementRecorder.enabled && launchMode == LaunchMode.DEVELOPMENT) {
                                    try {
                                        // do not interrupt during a hot replacement
                                        // as shutdown will abort and do nasty things.
                                        shouldInterrupt.set(false);
                                        if (LambdaHotReplacementRecorder.checkHotReplacement()) {
                                            // hot replacement happened in dev mode
                                            // so we requeue the request as quarkus will restart
                                            // and the message will not be processed
                                            // FYI: this requeue endpoint is something only the mock event server
                                            // implements
                                            requeue(baseUrl, requestId);
                                            return;
                                        }
                                    } finally {
                                        shouldInterrupt.set(true);
                                    }
                                }
                                String traceId = requestConnection
                                        .getHeaderField(AmazonLambdaApi.LAMBDA_TRACE_HEADER_KEY);
                                if (traceId != null) {
                                    System.setProperty(LAMBDA_TRACE_HEADER_PROP, traceId);
                                }
                                URL url = AmazonLambdaApi.invocationResponse(baseUrl, requestId);
                                if (isStream()) {
                                    HttpURLConnection responseConnection = responseStream(url);
                                    if (running.get()) {
                                        processRequest(requestConnection.getInputStream(),
                                                responseConnection.getOutputStream(), createContext(requestConnection));
                                        while (responseConnection.getInputStream().read() != -1) {
                                            // Read data
                                        }
                                    }
                                } else {
                                    Object input = null;
                                    if (running.get()) {
                                        LambdaInputReader inputReader = getInputReader();
                                        if (inputReader != null) {
                                            input = inputReader.readValue(requestConnection.getInputStream());
                                        }
                                        Object output = processRequest(input, createContext(requestConnection));
                                        postResponse(url, output);
                                    }
                                }
                            } catch (Exception e) {
                                if (abortGracefully(e)) {
                                    return;
                                }
                                if (shouldLog(e)) {
                                    log.error("Failed to run lambda (" + launchMode + ")", e);
                                }

                                postError(AmazonLambdaApi.invocationError(baseUrl, requestId),
                                        new FunctionError(e.getClass().getName(), e.getMessage()));
                                continue;
                            }

                        } catch (Exception e) {
                            if (!abortGracefully(e))
                                log.error("Error running lambda (" + launchMode + ")", e);
                            Application app = Application.currentApplication();
                            if (app != null) {
                                try {
                                    app.stop();
                                } catch (Exception ignored) {

                                }
                            }
                            return;
                        } finally {
                            MDC.remove(MDC_AWS_REQUEST_ID_KEY);
                            try {
                                requestConnection.getInputStream().close();
                            } catch (IOException ignored) {
                            }
                        }

                    }
                } catch (Exception e) {
                    try {
                        log.error("Lambda init error (" + launchMode + ")", e);
                        postError(AmazonLambdaApi.initError(baseUrl),
                                new FunctionError(e.getClass().getName(), e.getMessage()));
                    } catch (Exception ex) {
                        log.error("Failed to report init error (" + launchMode + ")", ex);
                    } finally {
                        // our main loop is done, time to shut down
                        Application app = Application.currentApplication();
                        if (app != null) {
                            log.error("Shutting down Quarkus application because of error (" + launchMode + ")");
                            app.stop();
                        }
                    }
                } finally {
                    log.info("Lambda polling thread complete (" + launchMode + ")");
                }
            }
        }, "Lambda Thread (" + launchMode + ")");
        pollingThread.setDaemon(true);
        context.addShutdownTask(() -> {
            running.set(false);
            try {
                // note that interrupting does not seem to be 100% reliable in unblocking the thread
                requestConnection.disconnect();
            } catch (Exception ignore) {
            }
            if (shouldInterrupt.get()) {
                pollingThread.interrupt();
            }
        });
        pollingThread.start();

    }

    /**
     * Invoke actual app code with unmarshalled input.
     *
     * @param input
     *        unmarshalled input (probably from json)
     * @param context
     *
     * @return Unmarshalled Java output (will probably be marshalled to json)
     *
     * @throws Exception
     */
    protected abstract Object processRequest(Object input, AmazonLambdaContext context) throws Exception;

    protected abstract void processRequest(InputStream input, OutputStream output, AmazonLambdaContext context)
            throws Exception;

    protected abstract LambdaInputReader getInputReader();

    protected abstract LambdaOutputWriter getOutputWriter();

    protected AmazonLambdaContext createContext(HttpURLConnection requestConnection) throws IOException {
        return new AmazonLambdaContext(requestConnection, cognitoIdReader, clientCtxReader);
    }

    private void checkQuarkusBootstrapped() {
        // todo we need a better way to do this.
        if (Application.currentApplication() == null) {
            throw new RuntimeException("Quarkus initialization error");
        }
        String[] args = {};
        Application.currentApplication().start(args);
    }

    protected void postResponse(URL url, Object response) throws IOException {
        HttpURLConnection responseConnection = (HttpURLConnection) url.openConnection();
        responseConnection.setRequestProperty(USER_AGENT, USER_AGENT_VALUE);
        if (response != null) {
            getOutputWriter().writeHeaders(responseConnection);
        }
        responseConnection.setDoOutput(true);
        responseConnection.setRequestMethod("POST");
        if (response != null) {
            getOutputWriter().writeValue(responseConnection.getOutputStream(), response);
        }
        while (responseConnection.getInputStream().read() != -1) {
            // Read data
        }
    }

    protected void requeue(String baseUrl, String requestId) throws IOException {
        URL url = AmazonLambdaApi.requeue(baseUrl, requestId);
        HttpURLConnection responseConnection = (HttpURLConnection) url.openConnection();
        responseConnection.setRequestProperty(USER_AGENT, USER_AGENT_VALUE);
        responseConnection.setDoOutput(true);
        responseConnection.setRequestMethod("POST");
        while (responseConnection.getInputStream().read() != -1) {
            // Read data
        }
    }

    protected void postError(URL url, Object response) throws IOException {
        HttpURLConnection responseConnection = (HttpURLConnection) url.openConnection();
        responseConnection.setRequestProperty(USER_AGENT, USER_AGENT_VALUE);
        responseConnection.setRequestProperty("Content-Type", "application/json");
        responseConnection.setDoOutput(true);
        responseConnection.setRequestMethod("POST");
        objectMapper.writeValue(responseConnection.getOutputStream(), response);
        while (responseConnection.getInputStream().read() != -1) {
            // Read data
        }
    }

    protected HttpURLConnection responseStream(URL url) throws IOException {
        HttpURLConnection responseConnection = (HttpURLConnection) url.openConnection();
        responseConnection.setRequestProperty(USER_AGENT, USER_AGENT_VALUE);
        responseConnection.setDoOutput(true);
        responseConnection.setRequestMethod("POST");
        return responseConnection;
    }

    boolean abortGracefully(Exception ex) {
        // if we are running in test mode, or native mode outside the lambda container, then don't output stack trace
        // for socket errors

        boolean lambdaEnv = System.getenv("AWS_LAMBDA_RUNTIME_API") != null;
        boolean testOrDevEnv = LaunchMode.current() == LaunchMode.TEST
                || LaunchMode.current() == LaunchMode.DEVELOPMENT;
        boolean graceful = ((ex instanceof SocketException) && testOrDevEnv)
                || (ex instanceof UnknownHostException && !lambdaEnv);

        if (graceful)
            log.warn(
                    "Aborting lambda poll loop: " + (lambdaEnv ? "no lambda container found" : "ending dev/test mode"));
        return graceful;
    }

}
