package io.quarkus.qrs.runtime.handlers;

import io.quarkus.qrs.runtime.core.QrsRequestContext;
import io.smallrye.mutiny.Uni;

public class UniResponseHandler implements RestHandler {

    @Override
    public void handle(QrsRequestContext requestContext) throws Exception {
        // FIXME: handle Response with entity being a Uni
        if (requestContext.getResult() instanceof Uni) {
            Uni<?> result = (Uni<?>) requestContext.getResult();
            requestContext.suspend();

            result.subscribe().with(v -> {
                requestContext.setResult(v);
                requestContext.resume();
            }, t -> {
                requestContext.setThrowable(t);
                requestContext.resume();
            });
        }
    }
}
