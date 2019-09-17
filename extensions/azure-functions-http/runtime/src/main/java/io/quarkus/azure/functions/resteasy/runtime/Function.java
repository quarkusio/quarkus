package io.quarkus.azure.functions.resteasy.runtime;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.HttpTrigger;

public class Function extends BaseFunction {

    public HttpResponseMessage run(
            @HttpTrigger(name = "req") HttpRequestMessage<Optional<byte[]>> request,
            final ExecutionContext context) {
        if (!started) {
            HttpResponseMessage.Builder responseBuilder = request
                    .createResponseBuilder(HttpStatus.valueOf(500)).body(
                            deploymentStatus.getBytes(StandardCharsets.UTF_8));
            return responseBuilder.build();
        }
        return dispatch(request);
    }
}
