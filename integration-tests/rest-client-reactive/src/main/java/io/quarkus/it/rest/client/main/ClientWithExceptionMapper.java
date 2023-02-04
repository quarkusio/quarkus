package io.quarkus.it.rest.client.main;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.quarkus.it.rest.client.main.MyResponseExceptionMapper.MyException;

@Path("/unprocessable")
@RegisterProvider(MyResponseExceptionMapper.class)
@RegisterRestClient(configKey = "w-exception-mapper")
public interface ClientWithExceptionMapper {
    @GET
    String get() throws MyException;
}
