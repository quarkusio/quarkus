package io.quarkus.amazon.lambda.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import io.quarkus.runtime.Application;
import io.quarkus.runtime.ShutdownContext;

public abstract class AbstractLambdaPollLoop {
    private static final Logger log = Logger.getLogger(AbstractLambdaPollLoop.class);

    private ObjectMapper objectMapper;
    private ObjectReader cognitoIdReader;
    private ObjectReader clientCtxReader;

    public AbstractLambdaPollLoop(ObjectMapper objectMapper, ObjectReader cognitoIdReader, ObjectReader clientCtxReader) {
        this.objectMapper = objectMapper;
        this.cognitoIdReader = cognitoIdReader;
        this.clientCtxReader = clientCtxReader;
    }

    protected abstract boolean isStream();

    public void startPollLoop(ShutdownContext context) {

        final AtomicBoolean running = new AtomicBoolean(true);
        final Thread pollingThread = new Thread(new Runnable() {
            @SuppressWarnings("unchecked")
            @Override
            public void run() {

                try {
                    checkQuarkusBootstrapped();
                    URL requestUrl = AmazonLambdaApi.invocationNext();
                    while (running.get()) {

                        HttpURLConnection requestConnection = null;
                        try {
                            requestConnection = (HttpURLConnection) requestUrl.openConnection();
                        } catch (IOException e) {
                            if (abortGracefully(e)) {
                                return;
                            }
                            throw e;
                        }
                        try {
                            String requestId = requestConnection.getHeaderField(AmazonLambdaApi.LAMBDA_RUNTIME_AWS_REQUEST_ID);
                            try {
                                String traceId = requestConnection.getHeaderField(AmazonLambdaApi.LAMBDA_TRACE_HEADER_KEY);
                                TraceId.setTraceId(traceId);
                                URL url = AmazonLambdaApi.invocationResponse(requestId);
                                if (isStream()) {
                                    HttpURLConnection responseConnection = responseStream(url);
                                    if (running.get()) {
                                        processRequest(requestConnection.getInputStream(), responseConnection.getOutputStream(),
                                                createContext(requestConnection));
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
                                log.error("Failed to run lambda", e);

                                postError(AmazonLambdaApi.invocationError(requestId),
                                        new FunctionError(e.getClass().getName(), e.getMessage()));
                                continue;
                            }

                        } catch (Exception e) {
                            if (!abortGracefully(e))
                                log.error("Error running lambda", e);
                            Application app = Application.currentApplication();
                            if (app != null) {
                                app.stop();
                            }
                            return;
                        } finally {
                            try {
                                requestConnection.getInputStream().close();
                            } catch (IOException e) {
                            }
                        }

                    }
                } catch (Exception e) {
                    try {
                        log.error("Lambda init error", e);
                        postError(AmazonLambdaApi.initError(), new FunctionError(e.getClass().getName(), e.getMessage()));
                    } catch (Exception ex) {
                        log.error("Failed to report init error", ex);
                    } finally {
                        // our main loop is done, time to shutdown
                        Application app = Application.currentApplication();
                        if (app != null) {
                            app.stop();
                        }
                    }
                }
            }
        }, "Lambda Thread");
        pollingThread.setDaemon(true);
        context.addShutdownTask(() -> {
            running.set(false);
            //note that this does not seem to be 100% reliable in unblocking the thread
            //which is why it is a daemon.
            pollingThread.interrupt();
        });
        pollingThread.start();

    }

    /**
     * Invoke actual app code with unmarshalled input.
     *
     * @param input unmarshalled input (probably from json)
     * @param context
     * @return Unmarshalled Java output (will probably be marshalled to json)
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
        responseConnection.setDoOutput(true);
        responseConnection.setRequestMethod("POST");
        if (response != null)
            getOutputWriter().writeValue(responseConnection.getOutputStream(), response);
        while (responseConnection.getInputStream().read() != -1) {
            // Read data
        }
    }

    protected void postError(URL url, Object response) throws IOException {
        HttpURLConnection responseConnection = (HttpURLConnection) url.openConnection();
        responseConnection.setDoOutput(true);
        responseConnection.setRequestMethod("POST");
        objectMapper.writeValue(responseConnection.getOutputStream(), response);
        while (responseConnection.getInputStream().read() != -1) {
            // Read data
        }
    }

    protected HttpURLConnection responseStream(URL url) throws IOException {
        HttpURLConnection responseConnection = (HttpURLConnection) url.openConnection();
        responseConnection.setDoOutput(true);
        responseConnection.setRequestMethod("POST");
        return responseConnection;
    }

    boolean abortGracefully(Exception ex) {
        // if we are running in test mode, or native mode outside of the lambda container, then don't output stack trace for socket errors

        boolean lambdaEnv = System.getenv("AWS_LAMBDA_RUNTIME_API") != null;
        boolean testEnv = System.getProperty(AmazonLambdaApi.QUARKUS_INTERNAL_AWS_LAMBDA_TEST_API) != null;
        boolean graceful = ((ex instanceof SocketException || ex instanceof ConnectException) && testEnv)
                || (ex instanceof UnknownHostException && !lambdaEnv);

        if (graceful)
            log.warn("Aborting lambda poll loop: " + (!lambdaEnv ? "no lambda container found" : "test mode"));
        return graceful;
    }

}
