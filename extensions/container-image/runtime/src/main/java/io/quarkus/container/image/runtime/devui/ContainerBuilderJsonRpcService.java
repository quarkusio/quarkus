package io.quarkus.container.image.runtime.devui;

import java.util.Map;

import io.quarkus.dev.console.DevConsoleManager;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;

public class ContainerBuilderJsonRpcService {

    public Multi<String> build(String type, String builder) {
        Map<String, String> params = Map.of(
                "quarkus.container-image.builder", builder,
                "quarkus.build.package-type", type);

        // For now, the JSON RPC are called on the event loop, but the action is blocking,
        // So, work around this by invoking the action on a worker thread.
        Multi<String> build = Uni.createFrom().item(() -> DevConsoleManager
                .<String> invoke("container-image-build-action", params))
                .runSubscriptionOn(Infrastructure.getDefaultExecutor()) // It's a blocking action.
                .toMulti();

        return Multi.createBy().concatenating()
                .streams(Multi.createFrom().item("started"), build);

    }

}
