package io.quarkus.amazon.lambda.runtime;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Various constants and util methods used for communication with the AWS API.
 */
public class AmazonLambdaApi {

    // Response Headers
    public static final String LAMBDA_RUNTIME_AWS_REQUEST_ID = "Lambda-Runtime-Aws-Request-Id";
    public static final String LAMBDA_RUNTIME_INVOKED_FUNCTION_ARN = "Lambda-Runtime-Invoked-Function-Arn";
    public static final String LAMBDA_RUNTIME_COGNITO_IDENTITY = "Lambda-Runtime-Cognito-Identity";
    public static final String LAMBDA_RUNTIME_CLIENT_CONTEXT = "Lambda-Runtime-Client-Context";
    public static final String LAMBDA_RUNTIME_DEADLINE_MS = "Lambda-Runtime-Deadline-Ms";

    // Test API
    public static final String QUARKUS_INTERNAL_AWS_LAMBDA_TEST_API = "quarkus-internal.aws-lambda.test-api";

    // API paths
    public static final String API_PROTOCOL = "http://";
    public static final String API_PATH_RUNTIME = "/2018-06-01/runtime/";
    public static final String API_PATH_INVOCATION = API_PATH_RUNTIME + "invocation/";
    public static final String API_PATH_INVOCATION_NEXT = API_PATH_INVOCATION + "next";
    public static final String API_PATH_INIT_ERROR = API_PATH_RUNTIME + "init/error";
    public static final String API_PATH_ERROR = "/error";
    public static final String API_PATH_RESPONSE = "/response";

    static URL invocationNext() throws MalformedURLException {
        return new URL(API_PROTOCOL + runtimeApi() + API_PATH_INVOCATION_NEXT);
    }

    static URL invocationError(String requestId) throws MalformedURLException {
        return new URL(API_PROTOCOL + runtimeApi() + API_PATH_INVOCATION + requestId + API_PATH_ERROR);
    }

    static URL invocationResponse(String requestId) throws MalformedURLException {
        return new URL(API_PROTOCOL + runtimeApi() + API_PATH_INVOCATION + requestId + API_PATH_RESPONSE);
    }

    static URL initError() throws MalformedURLException {
        return new URL(API_PROTOCOL + runtimeApi() + API_PATH_INIT_ERROR);
    }

    static String logGroupName() {
        return System.getenv("AWS_LAMBDA_LOG_GROUP_NAME");
    }

    static String functionMemorySize() {
        return System.getenv("AWS_LAMBDA_FUNCTION_MEMORY_SIZE");
    }

    static String logStreamName() {
        return System.getenv("AWS_LAMBDA_LOG_STREAM_NAME");
    }

    static String functionName() {
        return System.getenv("AWS_LAMBDA_FUNCTION_NAME");
    }

    static String functionVersion() {
        return System.getenv("AWS_LAMBDA_FUNCTION_VERSION");
    }

    private static String runtimeApi() {
        String testApi = System.getProperty(QUARKUS_INTERNAL_AWS_LAMBDA_TEST_API);
        if (testApi != null) {
            return testApi;
        }
        return System.getenv("AWS_LAMBDA_RUNTIME_API");
    }

}
