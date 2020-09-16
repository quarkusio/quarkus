package io.quarkus.smallrye.opentracing.runtime.graal;

import java.util.function.BooleanSupplier;

final class UndertowMissing implements BooleanSupplier {

    @Override
    public boolean getAsBoolean() {
        try {
            Class.forName("io.quarkus.undertow.runtime.UndertowDeploymentRecorder");
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }
}
