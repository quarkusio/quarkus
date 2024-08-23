package io.quarkus.websockets.next.test.upgrade;

import java.util.concurrent.atomic.AtomicInteger;

import io.quarkus.websockets.next.HttpUpgradeCheck;
import io.smallrye.mutiny.Uni;

public class OpeningHttpUpgradeCheck implements HttpUpgradeCheck {

    public static final AtomicInteger INVOKED = new AtomicInteger(0);

    @Override
    public Uni<CheckResult> perform(HttpUpgradeContext context) {
        if (shouldCheckUpgrade(context)) {
            INVOKED.incrementAndGet();
        }
        return CheckResult.permitUpgrade();
    }

    protected boolean shouldCheckUpgrade(HttpUpgradeContext context) {
        return context.httpRequest().path().contains("/opening");
    }
}
