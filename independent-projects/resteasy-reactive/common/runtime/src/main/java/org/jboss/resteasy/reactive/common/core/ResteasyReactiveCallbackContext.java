package org.jboss.resteasy.reactive.common.core;

import javax.ws.rs.container.CompletionCallback;

public interface ResteasyReactiveCallbackContext {
    public void registerCompletionCallback(CompletionCallback callback);
}
