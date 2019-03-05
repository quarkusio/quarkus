package io.quarkus.deployment.builditem;

import org.jboss.builder.item.SimpleBuildItem;

import io.quarkus.deployment.recording.BytecodeRecorderImpl;
import io.quarkus.runtime.MainArgsSupplier;

public final class MainArgsBuildItem extends SimpleBuildItem implements BytecodeRecorderImpl.ReturnedProxy, MainArgsSupplier {
    @Override
    public String __returned$proxy$key() {
        return MainArgsSupplier.class.getName();
    }

    @Override
    public boolean __static$$init() {
        return true;
    }

    @Override
    public String[] getArgs() {
        throw new IllegalStateException();
    }
}
