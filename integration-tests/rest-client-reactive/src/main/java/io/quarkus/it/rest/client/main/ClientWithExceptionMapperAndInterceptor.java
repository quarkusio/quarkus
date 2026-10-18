package io.quarkus.it.rest.client.main;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.quarkus.it.rest.client.main.MyResponseExceptionMapper.MyException;

@Path("/unprocessable")
@RegisterProvider(MyResponseExceptionMapper.class)
@RegisterRestClient(configKey = "w-exception-mapper-and-interceptor")
public interface ClientWithExceptionMapperAndInterceptor {
    @GET
    @Retry(maxRetries = 1, jitter = 0) // any interceptor would do here
    String get() throws MyException;
}
