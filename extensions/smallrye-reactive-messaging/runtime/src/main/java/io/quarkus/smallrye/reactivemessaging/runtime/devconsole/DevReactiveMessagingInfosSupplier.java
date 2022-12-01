package io.quarkus.smallrye.reactivemessaging.runtime.devconsole;

import java.util.function.Supplier;

public class DevReactiveMessagingInfosSupplier implements Supplier<DevReactiveMessagingInfos> {

    @Override
    public DevReactiveMessagingInfos get() {
        return new DevReactiveMessagingInfos();
    }

}
