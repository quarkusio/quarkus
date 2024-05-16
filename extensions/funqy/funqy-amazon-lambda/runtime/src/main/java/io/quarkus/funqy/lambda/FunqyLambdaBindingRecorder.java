package io.quarkus.funqy.lambda;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jboss.logging.Logger;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.module.SimpleModule;

import io.quarkus.amazon.lambda.runtime.AbstractLambdaPollLoop;
import io.quarkus.amazon.lambda.runtime.AmazonLambdaContext;
import io.quarkus.amazon.lambda.runtime.AmazonLambdaMapperRecorder;
import io.quarkus.amazon.lambda.runtime.LambdaInputReader;
import io.quarkus.amazon.lambda.runtime.LambdaOutputWriter;
import io.quarkus.arc.ManagedContext;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.funqy.lambda.config.FunqyAmazonBuildTimeConfig;
import io.quarkus.funqy.lambda.config.FunqyAmazonConfig;
import io.quarkus.funqy.lambda.event.AwsModule;
import io.quarkus.funqy.lambda.event.EventDeserializer;
import io.quarkus.funqy.lambda.event.EventProcessor;
import io.quarkus.funqy.lambda.model.FunqyMethod;
import io.quarkus.funqy.runtime.FunctionConstructor;
import io.quarkus.funqy.runtime.FunctionInvoker;
import io.quarkus.funqy.runtime.FunctionRecorder;
import io.quarkus.funqy.runtime.FunqyConfig;
import io.quarkus.funqy.runtime.FunqyServerResponse;
import io.quarkus.funqy.runtime.RequestContextImpl;
import io.quarkus.runtime.LaunchMode;
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
    private static EventProcessor eventProcessor;

    public void init(BeanContainer bc, FunqyAmazonBuildTimeConfig buildTimeConfig) {
        beanContainer = bc;
        FunctionConstructor.CONTAINER = bc;
        // We create a copy, because we register a custom deserializer for everything.
        ObjectMapper objectMapper = AmazonLambdaMapperRecorder.objectMapper.copy();
        EventDeserializer eventDeserializer = new EventDeserializer(buildTimeConfig);
        final SimpleModule simpleModule = new AwsModule();
        simpleModule.addDeserializer(Object.class, eventDeserializer);
        objectMapper.registerModule(simpleModule);

        for (FunctionInvoker invoker : FunctionRecorder.registry.invokers()) {
            ObjectReader reader = null;
            JavaType javaInputType = null;
            if (invoker.hasInput()) {
                javaInputType = objectMapper.constructType(invoker.getInputType());
                reader = objectMapper.readerFor(javaInputType);
            }
            ObjectWriter writer = null;
            JavaType javaOutputType = null;
            if (invoker.hasOutput()) {
                javaOutputType = objectMapper.constructType(invoker.getOutputType());
                writer = objectMapper.writerFor(javaOutputType);
            }
            invoker.getBindingContext().put(EventProcessor.class.getName(),
                    new EventProcessor(objectMapper, eventDeserializer,
                            new FunqyMethod(reader, writer, javaInputType, javaOutputType),
                            buildTimeConfig));
        }
    }

    public void chooseInvoker(FunqyConfig config, FunqyAmazonConfig amazonConfig) {
        // this is done at Runtime so that we can change it with an environment variable.
        if (config.export.isPresent()) {
            invoker = FunctionRecorder.registry.matchInvoker(config.export.get());
            if (invoker == null) {
                throw new RuntimeException("quarkus.funqy.export does not match a function: " + config.export.get());
            }
        } else if (FunctionRecorder.registry.invokers().size() == 0) {
            throw new RuntimeException("There are no functions to process lambda");

        } else if (FunctionRecorder.registry.invokers().size() > 1) {
            throw new RuntimeException("Too many functions.  You need to set quarkus.funqy.export");
        } else {
            invoker = FunctionRecorder.registry.invokers().iterator().next();
        }
        eventProcessor = (EventProcessor) invoker.getBindingContext().get(EventProcessor.class.getName());
        eventProcessor.init(amazonConfig);
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
        eventProcessor.handle(inputStream, outputStream, FunqyLambdaBindingRecorder::dispatch, context);
    }

    @SuppressWarnings("rawtypes")
    public void startPollLoop(ShutdownContext context, LaunchMode launchMode) {
        AbstractLambdaPollLoop loop = new AbstractLambdaPollLoop(AmazonLambdaMapperRecorder.objectMapper,
                AmazonLambdaMapperRecorder.cognitoIdReader, AmazonLambdaMapperRecorder.clientCtxReader, launchMode) {

            @Override
            protected Object processRequest(Object input, AmazonLambdaContext context) throws Exception {
                throw new RuntimeException("Unreachable");
            }

            @Override
            protected LambdaInputReader getInputReader() {
                throw new RuntimeException("Unreachable");
            }

            @Override
            protected LambdaOutputWriter getOutputWriter() {
                throw new RuntimeException("Unreachable");
            }

            @Override
            protected boolean isStream() {
                return true;
            }

            @Override
            protected void processRequest(InputStream input, OutputStream output, AmazonLambdaContext context)
                    throws Exception {
                handle(input, output, context);
            }
        };
        loop.startPollLoop(context);

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
