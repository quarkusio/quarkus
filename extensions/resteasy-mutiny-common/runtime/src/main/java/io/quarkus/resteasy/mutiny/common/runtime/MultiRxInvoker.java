package io.quarkus.resteasy.mutiny.common.runtime;

import javax.ws.rs.client.RxInvoker;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.BackPressureStrategy;

public interface MultiRxInvoker extends RxInvoker<Multi<?>> {

    BackPressureStrategy getBackPressureStrategy();

    void setBackPressureStrategy(BackPressureStrategy strategy);

}