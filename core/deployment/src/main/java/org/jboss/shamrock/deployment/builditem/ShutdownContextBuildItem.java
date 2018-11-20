package org.jboss.shamrock.deployment.builditem;

import org.jboss.builder.item.SimpleBuildItem;
import org.jboss.shamrock.deployment.recording.BytecodeRecorderImpl;
import org.jboss.shamrock.runtime.ShutdownContext;

/**
 * A build item that can be used to register shutdown tasks in runtime templates
 */
public final class ShutdownContextBuildItem extends SimpleBuildItem implements ShutdownContext, BytecodeRecorderImpl.ReturnedProxy {
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
