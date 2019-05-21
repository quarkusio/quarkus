package io.quarkus.resteasy.runtime;

import java.lang.reflect.Method;
import java.util.Objects;

import javax.ws.rs.container.ResourceInfo;

import org.jboss.resteasy.core.ResteasyContext;
import org.jboss.resteasy.spi.HttpRequest;

public class AsyncRequestStatusProvider implements io.quarkus.arc.AsyncRequestStatusProvider {

    @Override
    public boolean isCurrentRequestAsync(Method method) {
        HttpRequest resteasyHttpRequest = ResteasyContext.getContextData(HttpRequest.class);
        if(resteasyHttpRequest != null && resteasyHttpRequest.getAsyncContext().isSuspended()) {
            ResourceInfo resteasyResourceInfo  = ResteasyContext.getContextData(ResourceInfo.class);
            return resteasyResourceInfo != null
                    && Objects.equals(resteasyResourceInfo.getResourceMethod(), method);
        }
        return false;
    }

}
