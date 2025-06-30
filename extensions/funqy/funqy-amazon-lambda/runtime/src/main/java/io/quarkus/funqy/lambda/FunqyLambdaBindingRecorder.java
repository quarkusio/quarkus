package io.quarkus.funqy.lambda;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.quarkus.amazon.lambda.runtime.AbstractLambdaPollLoop;
import io.quarkus.amazon.lambda.runtime.AmazonLambdaContext;
import io.quarkus.amazon.lambda.runtime.AmazonLambdaMapperRecorder;
import io.quarkus.amazon.lambda.runtime.JacksonInputReader;
import io.quarkus.amazon.lambda.runtime.JacksonOutputWriter;
import io.quarkus.amazon.lambda.runtime.LambdaInputReader;
import io.quarkus.amazon.lambda.runtime.LambdaOutputWriter;
import io.quarkus.arc.ManagedContext;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.funqy.lambda.config.FunqyAmazonBuildTimeConfig;
import io.quarkus.funqy.lambda.config.FunqyAmazonConfig;
import io.quarkus.funqy.lambda.event.AwsEventInputReader;
import io.quarkus.funqy.lambda.event.AwsEventOutputWriter;
import io.quarkus.funqy.lambda.event.EventProcessor;
import io.quarkus.funqy.runtime.FunctionConstructor;
import io.quarkus.funqy.runtime.FunctionInvoker;
import io.quarkus.funqy.runtime.FunctionRecorder;
import io.quarkus.funqy.runtime.FunqyConfig;
import io.quarkus.funqy.runtime.FunqyServerResponse;
import io.quarkus.funqy.runtime.RequestContextImpl;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;

/**
 * Provides the runtime methods to bootstrap Quarkus Funq
 */
@Recorder
public class FunqyLambdaBindingRecorder {
    private static FunctionInvoker invoker;
    private static BeanContainer beanContainer;
    private static LambdaInputReader reader;
    private static LambdaOutputWriter writer;
    private static EventProcessor eventProcessor;

    private final RuntimeValue<FunqyConfig> runtimeConfig;
    private final FunqyAmazonBuildTimeConfig amazonBuildTimeConfig;
    private final RuntimeValue<FunqyAmazonConfig> amazonRuntimeConfig;

    public FunqyLambdaBindingRecorder(
            final RuntimeValue<FunqyConfig> runtimeConfig,
            final FunqyAmazonBuildTimeConfig amazonBuildTimeConfig,
            final RuntimeValue<FunqyAmazonConfig> amazonRuntimeConfig) {
        this.runtimeConfig = runtimeConfig;
        this.amazonBuildTimeConfig = amazonBuildTimeConfig;
        this.amazonRuntimeConfig = amazonRuntimeConfig;
    }

    public void init(BeanContainer bc) {
        beanContainer = bc;
        FunctionConstructor.CONTAINER = bc;
        ObjectMapper objectMapper = AmazonLambdaMapperRecorder.objectMapper;

        for (FunctionInvoker invoker : FunctionRecorder.registry.invokers()) {
            if (invoker.hasInput()) {
                JavaType javaInputType = objectMapper.constructType(invoker.getInputType());
                ObjectReader reader = objectMapper.readerFor(javaInputType);
                invoker.getBindingContext().put(ObjectReader.class.getName(), reader);
            }
            if (invoker.hasOutput()) {
                JavaType javaOutputType = objectMapper.constructType(invoker.getOutputType());
                ObjectWriter writer = objectMapper.writerFor(javaOutputType);
                invoker.getBindingContext().put(ObjectWriter.class.getName(), writer);
            }
        }
    }

    public void chooseInvoker() {
        // this is done at Runtime so that we can change it with an environment variable.
        if (runtimeConfig.getValue().export().isPresent()) {
            invoker = FunctionRecorder.registry.matchInvoker(runtimeConfig.getValue().export().get());
            if (invoker == null) {
                throw new RuntimeException(
                        "quarkus.funqy.export does not match a function: " + runtimeConfig.getValue().export().get());
            }
        } else if (FunctionRecorder.registry.invokers().size() == 0) {
            throw new RuntimeException("There are no functions to process lambda");

        } else if (FunctionRecorder.registry.invokers().size() > 1) {
            throw new RuntimeException("Too many functions.  You need to set quarkus.funqy.export");
        } else {
            invoker = FunctionRecorder.registry.invokers().iterator().next();
        }

        ObjectReader objectReader = null;
        if (invoker.hasInput()) {
            objectReader = (ObjectReader) invoker.getBindingContext().get(ObjectReader.class.getName());

            if (amazonBuildTimeConfig.advancedEventHandling().enabled()) {
                // We create a copy, because the mapper will be reconfigured for the advanced event handling,
                // and we do not want to adjust the ObjectMapper, which is available in arc context.
                ObjectMapper objectMapper = AmazonLambdaMapperRecorder.objectMapper.copy();
                reader = new AwsEventInputReader(objectMapper, objectReader, amazonBuildTimeConfig);
            } else {
                reader = new JacksonInputReader(objectReader);
            }

        }
        if (invoker.hasOutput()) {
            ObjectWriter objectWriter = (ObjectWriter) invoker.getBindingContext().get(ObjectWriter.class.getName());

            if (!amazonBuildTimeConfig.advancedEventHandling().enabled()) {
                writer = new JacksonOutputWriter(objectWriter);
            }
        }
        if (amazonBuildTimeConfig.advancedEventHandling().enabled()) {
            ObjectMapper objectMapper = AmazonLambdaMapperRecorder.objectMapper.copy();
            writer = new AwsEventOutputWriter(objectMapper);

            eventProcessor = new EventProcessor(objectReader, amazonBuildTimeConfig, amazonRuntimeConfig.getValue());
        }
    }

    /**
     * Called by JVM handler wrapper
     *
     * @param inputStream
     *        {@link InputStream} of the AWS SDK {@link com.amazonaws.services.lambda.runtime.RequestStreamHandler}
     * @param outputStream
     *        {@link OutputStream} of the AWS SDK {@link com.amazonaws.services.lambda.runtime.RequestStreamHandler}
     * @param context
     *        AWS context information provided to the Lambda
     * @throws IOException
     *         Is thrown in case the (de)serialization fails
     */
    public static void handle(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        Object input = null;
        if (invoker.hasInput()) {
            input = reader.readValue(inputStream);
        }
        FunqyServerResponse response = dispatch(input, context);

        Object value = response.getOutput().await().indefinitely();
        if (value != null) {
            writer.writeValue(outputStream, value);
        }
    }

    @SuppressWarnings("rawtypes")
    public void startPollLoop(ShutdownContext context, LaunchMode launchMode) {
        AbstractLambdaPollLoop loop = new AbstractLambdaPollLoop(AmazonLambdaMapperRecorder.objectMapper,
                AmazonLambdaMapperRecorder.cognitoIdReader, AmazonLambdaMapperRecorder.clientCtxReader, launchMode) {

            @Override
            protected Object processRequest(Object input, AmazonLambdaContext context) throws Exception {
                FunqyServerResponse response = dispatch(input, context);
                return response.getOutput().await().indefinitely();
            }

            @Override
            protected LambdaInputReader getInputReader() {
                return reader;
            }

            @Override
            protected LambdaOutputWriter getOutputWriter() {
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

    private static FunqyServerResponse dispatch(Object input, Context context) throws IOException {
        if (eventProcessor != null) {
            return eventProcessor.handle(input, FunqyLambdaBindingRecorder::dispatch, context);
        } else {
            return dispatch(input);
        }
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
