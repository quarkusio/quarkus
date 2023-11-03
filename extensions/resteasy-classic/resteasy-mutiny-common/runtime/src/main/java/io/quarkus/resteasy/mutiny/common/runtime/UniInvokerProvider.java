package io.quarkus.resteasy.mutiny.common.runtime;

import java.util.concurrent.ExecutorService;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.CompletionStageRxInvoker;
import jakarta.ws.rs.client.RxInvokerProvider;
import jakarta.ws.rs.client.SyncInvoker;

import org.jboss.resteasy.client.jaxrs.internal.ClientInvocationBuilder;

public class UniInvokerProvider implements RxInvokerProvider<UniRxInvoker> {

    @Override
    public boolean isProviderFor(Class<?> clazz) {
        return UniRxInvoker.class.equals(clazz);
    }

    @Override
    public UniRxInvoker getRxInvoker(SyncInvoker syncInvoker, ExecutorService executorService) {
        if (syncInvoker instanceof ClientInvocationBuilder) {
            ClientInvocationBuilder builder = (ClientInvocationBuilder) syncInvoker;
            CompletionStageRxInvoker completionStageRxInvoker = builder.rx();
            return new UniRxInvokerImpl(completionStageRxInvoker);
        } else {
            throw new ProcessingException("Expected a ClientInvocationBuilder");
        }
    }
}
