package io.quarkus.websockets.next.runtime.telemetry;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public sealed interface ConnectionInterceptor permits TracesConnectionInterceptor,
        ConnectionInterceptor.CompositeConnectionInterceptor, MetricsConnectionInterceptor {

    void connectionOpened();

    void connectionOpeningFailed(Throwable cause);

    /**
     * Way to pass a context between {@link ConnectionInterceptor} and telemetry endpoint decorators.
     *
     * @return unmodifiable map passed to decorators as {@link TelemetryWebSocketEndpointContext#contextData()}
     */
    Map<String, Object> getContextData();

    final class CompositeConnectionInterceptor implements ConnectionInterceptor {

        private final List<ConnectionInterceptor> leaves;

        CompositeConnectionInterceptor(List<ConnectionInterceptor> leaves) {
            this.leaves = List.copyOf(leaves);
        }

        @Override
        public void connectionOpened() {
            for (var leaf : leaves) {
                leaf.connectionOpened();
            }
        }

        @Override
        public void connectionOpeningFailed(Throwable cause) {
            for (var leaf : leaves) {
                leaf.connectionOpeningFailed(cause);
            }
        }

        @Override
        public Map<String, Object> getContextData() {
            Map<String, Object> map = new HashMap<>();
            for (var leaf : leaves) {
                map.putAll(leaf.getContextData());
            }
            return Collections.unmodifiableMap(map);
        }
    }
}
