package io.quarkus.amazon.lambda.runtime;

import static io.quarkus.amazon.lambda.runtime.AmazonLambdaApi.LAMBDA_RUNTIME_AWS_REQUEST_ID;
import static io.quarkus.amazon.lambda.runtime.AmazonLambdaApi.LAMBDA_RUNTIME_CLIENT_CONTEXT;
import static io.quarkus.amazon.lambda.runtime.AmazonLambdaApi.LAMBDA_RUNTIME_COGNITO_IDENTITY;
import static io.quarkus.amazon.lambda.runtime.AmazonLambdaApi.LAMBDA_RUNTIME_DEADLINE_MS;
import static io.quarkus.amazon.lambda.runtime.AmazonLambdaApi.LAMBDA_RUNTIME_INVOKED_FUNCTION_ARN;
import static io.quarkus.amazon.lambda.runtime.AmazonLambdaApi.functionMemorySize;
import static io.quarkus.amazon.lambda.runtime.AmazonLambdaApi.functionName;
import static io.quarkus.amazon.lambda.runtime.AmazonLambdaApi.functionVersion;
import static io.quarkus.amazon.lambda.runtime.AmazonLambdaApi.logGroupName;
import static io.quarkus.amazon.lambda.runtime.AmazonLambdaApi.logStreamName;

import java.io.IOException;
import java.net.HttpURLConnection;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.LambdaRuntime;
import com.fasterxml.jackson.databind.ObjectReader;

public class AmazonLambdaContext implements Context {

    private String awsRequestId;
    private String logGroupName;
    private String logStreamName;
    private String functionName;
    private String functionVersion;
    private String invokedFunctionArn;
    private CognitoIdentity cognitoIdentity;
    private ClientContext clientContext;
    private long runtimeDeadlineMs = 0;
    private int memoryLimitInMB;
    private LambdaLogger logger;

    public AmazonLambdaContext(HttpURLConnection request, ObjectReader cognitoReader, ObjectReader clientCtxReader)
            throws IOException {
        awsRequestId = request.getHeaderField(LAMBDA_RUNTIME_AWS_REQUEST_ID);
        logGroupName = logGroupName();
        logStreamName = logStreamName();
        functionName = functionName();
        functionVersion = functionVersion();
        invokedFunctionArn = request.getHeaderField(LAMBDA_RUNTIME_INVOKED_FUNCTION_ARN);

        String cognitoIdentityHeader = request.getHeaderField(LAMBDA_RUNTIME_COGNITO_IDENTITY);
        if (cognitoIdentityHeader != null) {
            cognitoIdentity = cognitoReader.readValue(cognitoIdentityHeader);
        }

        String clientContextHeader = request.getHeaderField(LAMBDA_RUNTIME_CLIENT_CONTEXT);
        if (clientContextHeader != null) {
            clientContext = clientCtxReader.readValue(clientContextHeader);
        }

        String functionMemorySize = functionMemorySize();
        memoryLimitInMB = functionMemorySize != null ? Integer.valueOf(functionMemorySize) : 0;

        String runtimeDeadline = request.getHeaderField(LAMBDA_RUNTIME_DEADLINE_MS);
        if (runtimeDeadline != null) {
            runtimeDeadlineMs = Long.valueOf(runtimeDeadline);
        }
        logger = LambdaRuntime.getLogger();
    }

    @Override
    public String getAwsRequestId() {
        return awsRequestId;
    }

    @Override
    public String getLogGroupName() {
        return logGroupName;
    }

    @Override
    public String getLogStreamName() {
        return logStreamName;
    }

    @Override
    public String getFunctionName() {
        return functionName;
    }

    @Override
    public String getFunctionVersion() {
        return functionVersion;
    }

    @Override
    public String getInvokedFunctionArn() {
        return invokedFunctionArn;
    }

    @Override
    public CognitoIdentity getIdentity() {
        return cognitoIdentity;
    }

    @Override
    public ClientContext getClientContext() {
        return clientContext;
    }

    @Override
    public int getRemainingTimeInMillis() {
        return (int) (runtimeDeadlineMs - Math.round(System.nanoTime() / 1_000_000d));
    }

    @Override
    public int getMemoryLimitInMB() {
        return memoryLimitInMB;
    }

    @Override
    public LambdaLogger getLogger() {
        return logger;
    }
}
