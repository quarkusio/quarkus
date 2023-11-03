package io.quarkus.azure.functions.resteasy.runtime;

import java.util.Optional;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

public class Function extends BaseFunction {

    public static final String QUARKUS_HTTP = "QuarkusHttp";

    @FunctionName(QUARKUS_HTTP)
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", dataType = "binary", methods = { HttpMethod.GET, HttpMethod.HEAD, HttpMethod.POST,
                    HttpMethod.PUT,
                    HttpMethod.OPTIONS }, route = "{*path}", authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        return dispatch(request);
    }
}
