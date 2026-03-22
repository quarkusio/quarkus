package io.quarkus.reactive.transaction;

import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;

public interface ReactiveResource {

    Uni<?> beforeCommit(Context context);

    Uni<?> afterCommit(Context context);
}
