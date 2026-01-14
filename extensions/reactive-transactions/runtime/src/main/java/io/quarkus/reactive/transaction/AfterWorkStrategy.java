package io.quarkus.reactive.transaction;

import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;

public interface AfterWorkStrategy<T> {
    Uni<T> getAfterWorkActions(Context context);
}
