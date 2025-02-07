package io.quarkus.azure.functions.resteasy.runtime;

import static com.microsoft.azure.functions.HttpMethod.CONNECT;
import static com.microsoft.azure.functions.HttpMethod.DELETE;
import static com.microsoft.azure.functions.HttpMethod.GET;
import static com.microsoft.azure.functions.HttpMethod.HEAD;
import static com.microsoft.azure.functions.HttpMethod.OPTIONS;
import static com.microsoft.azure.functions.HttpMethod.PATCH;
import static com.microsoft.azure.functions.HttpMethod.POST;
import static com.microsoft.azure.functions.HttpMethod.PUT;
import static com.microsoft.azure.functions.HttpMethod.TRACE;

import java.util.Optional;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

public class Function extends BaseFunction {

    public static final String QUARKUS_HTTP = "QuarkusHttp";

    @FunctionName(QUARKUS_HTTP)
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", dataType = "binary", route = "{*path}", authLevel = AuthorizationLevel.ANONYMOUS, methods = {
                    GET, HEAD, POST, PUT, DELETE, CONNECT, OPTIONS, TRACE,
                    PATCH }) HttpRequestMessage<Optional<String>> request,
            ExecutionContext context) {

        return dispatch(request);
    }
}
