package io.quarkus.rest.client.reactive.registerclientheaders;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.quarkus.rest.client.reactive.ComputedParamContext;
import io.quarkus.rest.client.reactive.NotBody;
import io.quarkus.rest.client.reactive.TestJacksonBasicMessageBodyReader;

@RegisterRestClient
@RegisterClientHeaders(MyHeadersFactory.class)
@ClientHeaderParam(name = "my-header", value = "constant-header-value")
@ClientHeaderParam(name = "computed-header", value = "{io.quarkus.rest.client.reactive.registerclientheaders.ComputedHeader.get}")
@ClientHeaderParam(name = "Content-Type", value = "{io.quarkus.rest.client.reactive.registerclientheaders.ComputedHeader.contentType}")
@RegisterProvider(TestJacksonBasicMessageBodyReader.class)
public interface MultipleHeadersBindingClient {
    @GET
    @Path("/describe-request")
    @ClientHeaderParam(name = "header-from-properties", value = "${header.value}")
    RequestData call(@HeaderParam("jaxrs-style-header") String headerValue, @NotBody String usedForComputingContentType);

    @GET
    @Path("/describe-request")
    @ClientHeaderParam(name = "header-from-properties", value = "${header.value}")
    @ClientHeaderParam(name = "Content-Type", value = "{calculateContentType}")
    RequestData call(@HeaderParam("jaxrs-style-header") String headerValue, @NotBody String usedForComputingContentType,
            String unusedBody);

    @GET
    @Path("/describe-request")
    @ClientHeaderParam(name = "header-from-properties", value = "${header.value}")
    @ClientHeaderParam(name = "Content-Type", value = "application/json;param2={usedForComputingContentType}")
    RequestData call2(@HeaderParam("jaxrs-style-header") String headerValue, @NotBody String usedForComputingContentType,
            String unusedBody);

    default String calculateContentType(ComputedParamContext context) {
        return "application/json;param2=" + context.methodParameters().get(1).value();
    }
}
