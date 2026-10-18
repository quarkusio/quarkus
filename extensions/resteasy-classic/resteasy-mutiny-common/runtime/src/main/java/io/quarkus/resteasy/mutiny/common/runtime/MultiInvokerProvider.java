package io.quarkus.resteasy.mutiny.common.runtime;

import java.util.concurrent.ExecutorService;

import jakarta.ws.rs.client.RxInvokerProvider;
import jakarta.ws.rs.client.SyncInvoker;
import jakarta.ws.rs.client.WebTarget;

public class MultiInvokerProvider implements RxInvokerProvider<MultiRxInvoker> {
    WebTarget target;

    @Override
    public boolean isProviderFor(Class<?> clazz) {
        return MultiRxInvoker.class.equals(clazz);
    }

    @Override
    public MultiRxInvoker getRxInvoker(SyncInvoker syncInvoker, ExecutorService executorService) {
        return new MultiRxInvokerImpl(syncInvoker, executorService);
    }

    public WebTarget getTarget() {
        return target;
    }

    public void setTarget(WebTarget target) {
        this.target = target;
    }

}
