package io.quarkus.spring.web.resteasy.classic.runtime;

import jakarta.ws.rs.ConstrainedTo;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.Provider;

import org.springframework.http.ResponseEntity;

@Provider
@ConstrainedTo(RuntimeType.SERVER)
public class ResponseEntityFeature implements DynamicFeature {

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        if (!ResponseEntity.class.equals(resourceInfo.getResourceMethod().getReturnType())) {
            return;
        }

        context.register(new ResponseEntityContainerResponseFilter());
    }
}