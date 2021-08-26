package io.quarkus.vertx.http.runtime.devmode;

import java.util.function.Supplier;

public class HasDevServicesSupplier implements Supplier<Boolean> {

    private boolean hasDevServices = false;

    public HasDevServicesSupplier() {
    }

    public HasDevServicesSupplier(final boolean hasDevServices) {
        this.hasDevServices = hasDevServices;
    }

    public boolean isHasDevServices() {
        return hasDevServices;
    }

    public void setHasDevServices(boolean hasDevServices) {
        this.hasDevServices = hasDevServices;
    }

    @Override
    public Boolean get() {
        return this.hasDevServices;
    }

}
