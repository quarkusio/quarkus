package io.quarkus.websockets.next.deployment;

import java.util.List;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.websockets.next.deployment.WebSocketServerProcessor.GlobalErrorHandler;

final class GlobalErrorHandlersBuildItem extends SimpleBuildItem {

    final List<GlobalErrorHandler> handlers;

    GlobalErrorHandlersBuildItem(List<GlobalErrorHandler> handlers) {
        this.handlers = handlers;
    }

}
