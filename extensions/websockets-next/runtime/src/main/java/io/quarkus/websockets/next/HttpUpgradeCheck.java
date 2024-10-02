package io.quarkus.websockets.next;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.VertxContextSupport;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;

/**
 * A check that controls which requests are allowed to upgrade the HTTP connection to a WebSocket connection.
 * CDI beans implementing this interface are invoked on every request.
 * The CDI beans implementing `HttpUpgradeCheck` interface can be either `@ApplicationScoped`, `@Singleton`
 * or `@Dependent` beans, but never the `@RequestScoped` beans.
 * <p>
 * The checks are called orderly according to a bean priority.
 * When no priority is declared (for example with the `@jakarta.annotation.Priority` annotation), default priority is used.
 * If one of the checks rejects the upgrade, remaining checks are not called.
 */
public interface HttpUpgradeCheck {

    /**
     * This method inspects HTTP Upgrade context and either allows or denies upgrade to a WebSocket connection.
     * <p>
     * Use {@link VertxContextSupport#executeBlocking(java.util.concurrent.Callable)} in order to execute some blocking code in
     * the check.
     *
     * @param context {@link HttpUpgradeContext}
     * @return check result; must never be null
     */
    Uni<CheckResult> perform(HttpUpgradeContext context);

    /**
     * Determines WebSocket endpoints this check is applied to.
     *
     * @param endpointId WebSocket endpoint id, @see {@link WebSocket#endpointId()} for more information
     * @return true if this check should be applied on a WebSocket endpoint with given id
     */
    default boolean appliesTo(String endpointId) {
        return true;
    }

    /**
     * @param httpRequest {@link HttpServerRequest}; the HTTP 1.X request employing the 'Upgrade' header
     * @param securityIdentity {@link SecurityIdentity}; the identity is null if the Quarkus Security extension is absent
     * @param endpointId {@link WebSocket#endpointId()}
     */
    record HttpUpgradeContext(HttpServerRequest httpRequest, SecurityIdentity securityIdentity, String endpointId) {
    }

    final class CheckResult {

        private static final CheckResult PERMIT_UPGRADE = new CheckResult(true, null, Map.of());

        private final boolean upgradePermitted;
        private final int httpResponseCode;
        private final Map<String, List<String>> responseHeaders;

        private CheckResult(boolean upgradePermitted, Integer httpResponseCode, Map<String, List<String>> responseHeaders) {
            this.upgradePermitted = upgradePermitted;
            this.httpResponseCode = httpResponseCode == null ? 500 : httpResponseCode;
            this.responseHeaders = toUnmodifiableMap(responseHeaders);
        }

        public boolean isUpgradePermitted() {
            return upgradePermitted;
        }

        public int getHttpResponseCode() {
            return httpResponseCode;
        }

        public Map<String, List<String>> getResponseHeaders() {
            return this.responseHeaders;
        }

        public CheckResult withHeaders(Map<String, List<String>> responseHeaders) {
            if (responseHeaders == null || responseHeaders.isEmpty()) {
                return this;
            }

            var newHeaders = new HashMap<>(responseHeaders);
            this.responseHeaders.forEach((k, v) -> newHeaders.put(k, merge(v, newHeaders.get(k))));

            return new CheckResult(this.upgradePermitted, this.httpResponseCode, newHeaders);
        }

        public static Uni<CheckResult> rejectUpgrade(Integer httpResponseCode, Map<String, List<String>> responseHeaders) {
            return Uni.createFrom().item(rejectUpgradeSync(httpResponseCode, responseHeaders));
        }

        public static Uni<CheckResult> rejectUpgrade(Integer httpResponseCode) {
            return rejectUpgrade(httpResponseCode, null);
        }

        public static CheckResult rejectUpgradeSync(Integer httpResponseCode) {
            return rejectUpgradeSync(httpResponseCode, null);
        }

        public static CheckResult rejectUpgradeSync(Integer httpResponseCode, Map<String, List<String>> responseHeaders) {
            return new CheckResult(false, httpResponseCode, responseHeaders);
        }

        public static Uni<CheckResult> permitUpgrade(Map<String, List<String>> responseHeaders) {
            return Uni.createFrom().item(permitUpgradeSync(responseHeaders));
        }

        public static CheckResult permitUpgradeSync(Map<String, List<String>> responseHeaders) {
            return new CheckResult(true, null, responseHeaders);
        }

        public static Uni<CheckResult> permitUpgrade() {
            return Uni.createFrom().item(permitUpgradeSync());
        }

        public static CheckResult permitUpgradeSync() {
            return PERMIT_UPGRADE;
        }

        /**
         * Merge two lists.
         *
         * @param a never null
         * @param b nullable
         * @return list containing both {@code a} and {@code b} (if present)
         */
        private static List<String> merge(List<String> a, List<String> b) {
            if (b == null || b.isEmpty()) {
                return a;
            }
            return Stream.concat(a.stream(), b.stream()).toList();
        }

        private static Map<String, List<String>> toUnmodifiableMap(Map<String, List<String>> responseHeaders) {
            if (responseHeaders == null || responseHeaders.isEmpty()) {
                return Map.of();
            }
            var mutableMap = new HashMap<>(responseHeaders);
            mutableMap.replaceAll((k, v) -> List.copyOf(v));
            return Map.copyOf(mutableMap);
        }
    }
}
