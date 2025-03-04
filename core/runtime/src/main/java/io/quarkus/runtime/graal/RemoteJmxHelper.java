package io.quarkus.runtime.graal;

import java.util.function.BooleanSupplier;

public class RemoteJmxHelper {
    private static boolean absent;

    public static void setJmxServerNotIncluded(boolean value) {
        absent = value;
    }

    public static class JmxServerNotIncluded implements BooleanSupplier {

        @Override
        public boolean getAsBoolean() {
            return RemoteJmxHelper.absent;
        }
    }
}
