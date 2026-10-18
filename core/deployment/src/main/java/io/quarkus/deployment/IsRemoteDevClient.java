package io.quarkus.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.dev.spi.DevModeType;

/**
 * boolean supplier that returns true if the application is the local side
 * of remote dev mode. Intended for use with {@link BuildStep#onlyIf()}
 */
public class IsRemoteDevClient implements BooleanSupplier {

    private final DevModeType devModeType;

    public IsRemoteDevClient(DevModeType devModeType) {
        this.devModeType = devModeType;
    }

    @Override
    public boolean getAsBoolean() {
        return devModeType == DevModeType.REMOTE_LOCAL_SIDE;
    }
}
