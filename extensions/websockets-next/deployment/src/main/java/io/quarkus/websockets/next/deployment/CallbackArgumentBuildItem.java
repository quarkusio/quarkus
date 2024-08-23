package io.quarkus.websockets.next.deployment;

import io.quarkus.builder.item.MultiBuildItem;

final class CallbackArgumentBuildItem extends MultiBuildItem {

    private final CallbackArgument provider;

    CallbackArgumentBuildItem(CallbackArgument provider) {
        this.provider = provider;
    }

    CallbackArgument getProvider() {
        return provider;
    }

}
