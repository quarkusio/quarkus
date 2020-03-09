package io.quarkus.deployment.builditem;

import java.util.function.Supplier;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.recording.BytecodeRecorderImpl;
import io.quarkus.runtime.StartupContext;

/**
 * A build item that represents the raw command line arguments as they were passed to the application.
 *
 * No filtering is done on these parameters.
 */
public final class RawCommandLineArgumentsBuildItem extends SimpleBuildItem
        implements BytecodeRecorderImpl.ReturnedProxy, Supplier<String[]> {

    @Override
    public String __returned$proxy$key() {
        return StartupContext.RAW_COMMAND_LINE_ARGS;
    }

    @Override
    public boolean __static$$init() {
        return true;
    }

    @Override
    public String[] get() {
        throw new IllegalStateException("Can only be called at runtime");
    }
}
