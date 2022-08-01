package org.jboss.resteasy.reactive.common.core;

import javax.ws.rs.container.CompletionCallback;
import javax.ws.rs.container.ConnectionCallback;

public interface ResteasyReactiveCallbackContext {
    public void registerCompletionCallback(CompletionCallback callback);

    public void registerConnectionCallback(ConnectionCallback callback);
}
