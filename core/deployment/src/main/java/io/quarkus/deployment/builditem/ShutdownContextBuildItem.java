package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.recording.BytecodeRecorderImpl;
import io.quarkus.runtime.ShutdownContext;

/**
 * A build item that can be used to register shutdown tasks in runtime recorders.
 */
public final class ShutdownContextBuildItem extends SimpleBuildItem
        implements ShutdownContext, BytecodeRecorderImpl.ReturnedProxy {
    @Override
    public String __returned$proxy$key() {
        return ShutdownContext.class.getName();
    }

    @Override
    public boolean __static$$init() {
        return true;
    }

    @Override
    public void addShutdownTask(Runnable runnable) {
        throw new IllegalStateException();
    }
}
