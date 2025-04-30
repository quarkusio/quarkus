package io.quarkus.websockets.next.deployment;

import java.util.List;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.websockets.next.deployment.Callback.Target;
import io.quarkus.websockets.next.deployment.WebSocketProcessor.GlobalErrorHandler;

final class GlobalErrorHandlersBuildItem extends SimpleBuildItem {

    final List<GlobalErrorHandler> handlers;

    GlobalErrorHandlersBuildItem(List<GlobalErrorHandler> handlers) {
        this.handlers = handlers;
    }

    List<GlobalErrorHandler> forServer() {
        return handlers.stream().filter(h -> h.callback().isServer() || h.callback().target == Target.UNDEFINED).toList();
    }

    List<GlobalErrorHandler> forClient() {
        return handlers.stream().filter(h -> h.callback().isClient() || h.callback().target == Target.UNDEFINED).toList();
    }
}
