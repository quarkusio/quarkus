package io.quarkus.devui.observability.store;

/**
 * Decides when a telemetry buffer has grown past its configured bound. Pluggable
 * so future signals can bound by byte-size or time instead of element count.
 */
public interface BoundingStrategy {

    /**
     * @param currentSize the number of elements currently held
     * @return true if the buffer now holds more than its bound allows and the
     *         oldest element(s) must be evicted
     */
    boolean exceedsBound(int currentSize);
}
