package io.quarkus.websockets.next.runtime;

import static io.netty.handler.codec.http.HttpHeaderNames.CACHE_CONTROL;
import static io.netty.handler.codec.http.HttpHeaderNames.LOCATION;

import java.util.List;
import java.util.Map;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.spi.runtime.MethodDescription;
import io.quarkus.security.spi.runtime.SecurityCheck;
import io.quarkus.websockets.next.HttpUpgradeCheck;
import io.smallrye.mutiny.Uni;

public class SecurityHttpUpgradeCheck implements HttpUpgradeCheck {

    public static final int BEAN_PRIORITY = Integer.MAX_VALUE - 100;

    private final String redirectUrl;
    private final Map<String, SecurityCheck> endpointToCheck;

    SecurityHttpUpgradeCheck(String redirectUrl, Map<String, SecurityCheck> endpointToCheck) {
        this.redirectUrl = redirectUrl;
        this.endpointToCheck = Map.copyOf(endpointToCheck);
    }

    @Override
    public Uni<CheckResult> perform(HttpUpgradeContext context) {
        return context.securityIdentity().chain(identity -> endpointToCheck.get(context.endpointId())
                .nonBlockingApply(identity, (MethodDescription) null, null)
                .replaceWith(CheckResult::permitUpgradeSync)
                .onFailure(SecurityException.class).recoverWithItem(this::rejectUpgrade));
    }

    @Override
    public boolean appliesTo(String endpointId) {
        return endpointToCheck.containsKey(endpointId);
    }

    private CheckResult rejectUpgrade(Throwable throwable) {
        if (redirectUrl != null) {
            return CheckResult.rejectUpgradeSync(302,
                    Map.of(LOCATION.toString(), List.of(redirectUrl),
                            CACHE_CONTROL.toString(), List.of("no-store")));
        } else if (throwable instanceof ForbiddenException) {
            return CheckResult.rejectUpgradeSync(403);
        } else {
            return CheckResult.rejectUpgradeSync(401);
        }
    }

}
