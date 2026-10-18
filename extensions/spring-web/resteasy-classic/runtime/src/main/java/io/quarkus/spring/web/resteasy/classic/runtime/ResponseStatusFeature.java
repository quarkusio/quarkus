package io.quarkus.spring.web.resteasy.classic.runtime;

import jakarta.ws.rs.ConstrainedTo;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.Provider;

import org.jboss.resteasy.spi.HttpResponseCodes;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Provider
@ConstrainedTo(RuntimeType.SERVER)
public class ResponseStatusFeature implements DynamicFeature {

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        ResponseStatus responseStatus = resourceInfo.getResourceMethod().getAnnotation(ResponseStatus.class);
        if (responseStatus == null) {
            return;
        }

        HttpStatus httpStatus = responseStatus.code();
        if (httpStatus == HttpStatus.INTERNAL_SERVER_ERROR) {
            httpStatus = responseStatus.value();
        }

        context.register(new ResponseStatusContainerResponseFilter(
                void.class.equals(resourceInfo.getResourceMethod().getReturnType())
                        ? HttpResponseCodes.SC_NO_CONTENT
                        : HttpResponseCodes.SC_OK,
                httpStatus.value()));
    }
}
