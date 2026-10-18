package io.quarkus.websockets.next.runtime;

import java.util.Map;
import java.util.function.Consumer;

import io.quarkus.websockets.next.HttpUpgradeCheck;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

public final class HttpUpgradeSecurityInterceptor implements HttpUpgradeCheck {

    public static final int BEAN_PRIORITY = SecurityHttpUpgradeCheck.BEAN_PRIORITY + 10;
    private final Map<String, Consumer<RoutingContext>> endpointIdToInterceptor;

    HttpUpgradeSecurityInterceptor(Map<String, Consumer<RoutingContext>> endpointIdToInterceptor) {
        this.endpointIdToInterceptor = Map.copyOf(endpointIdToInterceptor);
    }

    @Override
    public Uni<CheckResult> perform(HttpUpgradeContext context) {
        if (context instanceof HttpUpgradeContextImpl impl) {
            endpointIdToInterceptor.get(context.endpointId()).accept(impl.routingContext());
            return CheckResult.permitUpgrade();
        } else {
            // this shouldn't happen and if anyone tries to change the 'impl', tests will fail
            return CheckResult.rejectUpgrade(500);
        }
    }

    @Override
    public boolean appliesTo(String endpointId) {
        return endpointIdToInterceptor.containsKey(endpointId);
    }

}
