package io.quarkus.funqy.lambda;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import org.jboss.logging.Logger;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.quarkus.amazon.lambda.runtime.AbstractLambdaPollLoop;
import io.quarkus.amazon.lambda.runtime.AmazonLambdaContext;
import io.quarkus.amazon.lambda.runtime.AmazonLambdaMapperRecorder;
import io.quarkus.arc.ManagedContext;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.funqy.runtime.FunctionConstructor;
import io.quarkus.funqy.runtime.FunctionInvoker;
import io.quarkus.funqy.runtime.FunctionRecorder;
import io.quarkus.funqy.runtime.FunqyServerResponse;
import io.quarkus.funqy.runtime.RequestContextImpl;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;

/**
 * Provides the runtime methods to bootstrap Quarkus Funq
 */
@Recorder
public class FunqyLambdaBindingRecorder {
    private static final Logger log = Logger.getLogger(FunqyLambdaBindingRecorder.class);

    private static FunctionInvoker invoker;
    private static BeanContainer beanContainer;
    private static ObjectReader reader;
    private static ObjectWriter writer;

    public void init(BeanContainer bc, String function) {
        beanContainer = bc;
        ObjectMapper objectMapper = AmazonLambdaMapperRecorder.objectMapper;
        invoker = FunctionRecorder.registry.matchInvoker(function);
        if (invoker.hasInput()) {
            reader = objectMapper.readerFor(invoker.getInputType());
        }
        if (invoker.hasOutput()) {
            writer = objectMapper.writerFor(invoker.getOutputType());
        }
        FunctionConstructor.CONTAINER = bc;

    }

    /**
     * Called by JVM handler wrapper
     *
     * @param inputStream
     * @param outputStream
     * @param context
     * @throws IOException
     */
    public static void handle(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        Object input = null;
        if (invoker.hasInput()) {
            input = reader.readValue(inputStream);
        }
        FunqyServerResponse response = dispatch(input);

        Object value = awaitCompletionStage(response.getOutput());
        if (value != null) {
            writer.writeValue(outputStream, value);
        }

    }

    @SuppressWarnings("rawtypes")
    public void startPollLoop(ShutdownContext context) {
        AbstractLambdaPollLoop loop = new AbstractLambdaPollLoop(AmazonLambdaMapperRecorder.objectMapper,
                AmazonLambdaMapperRecorder.cognitoIdReader, AmazonLambdaMapperRecorder.cognitoIdReader) {

            @Override
            protected Object processRequest(Object input, AmazonLambdaContext context) throws Exception {
                FunqyServerResponse response = dispatch(input);
                return awaitCompletionStage(response.getOutput());
            }

            @Override
            protected ObjectReader getInputReader() {
                return reader;
            }

            @Override
            protected ObjectWriter getOutputWriter() {
                return writer;
            }

            @Override
            protected boolean isStream() {
                return false;
            }

            @Override
            protected void processRequest(InputStream input, OutputStream output, AmazonLambdaContext context)
                    throws Exception {
                throw new RuntimeException("Unreachable!");
            }
        };
        loop.startPollLoop(context);

    }

    private static <T> T awaitCompletionStage(CompletionStage<T> output) {
        T val;
        try {
            val = output.toCompletableFuture().get();
        } catch (ExecutionException ex) {
            Throwable inner = ex.getCause();
            if (inner instanceof RuntimeException) {
                throw (RuntimeException) inner;
            } else {
                throw new RuntimeException(inner);
            }
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
        return val;
    }

    private static FunqyServerResponse dispatch(Object input) {
        ManagedContext requestContext = beanContainer.requestContext();
        requestContext.activate();
        try {
            FunqyRequestImpl funqyRequest = new FunqyRequestImpl(new RequestContextImpl(), input);
            FunqyResponseImpl funqyResponse = new FunqyResponseImpl();
            invoker.invoke(funqyRequest, funqyResponse);
            return funqyResponse;
        } finally {
            if (requestContext.isActive()) {
                requestContext.terminate();
            }
        }
    }

}
