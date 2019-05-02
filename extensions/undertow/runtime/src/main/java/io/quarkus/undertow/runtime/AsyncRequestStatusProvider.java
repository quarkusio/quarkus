package io.quarkus.undertow.runtime;

import io.undertow.servlet.handlers.ServletRequestContext;

public class AsyncRequestStatusProvider implements io.quarkus.arc.AsyncRequestStatusProvider {

    @Override
    public boolean isCurrentRequestAsync() {
        ServletRequestContext servletRequestContext = ServletRequestContext.current();
        return servletRequestContext != null && servletRequestContext.getServletRequest().isAsyncStarted();
    }

}
