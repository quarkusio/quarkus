package io.smallrye.metrics.api;

import java.util.function.Supplier;

import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Tag;

import io.smallrye.common.annotation.Experimental;

/**
 * Temporary tool to support function gauges in 2.4.x even though they become part of the official API in 3.0.
 * This is scheduled to be removed along with the upgrade to SmallRye Metrics 3.0.
 */
@Experimental("Temporarily backported API from Metrics 3.0 to support it in 2.4.x")
@Deprecated
public interface FunctionGaugeSupport {

    <T extends Number> Gauge<T> gauge(String name, Supplier<T> supplier, Tag... tags);

}
