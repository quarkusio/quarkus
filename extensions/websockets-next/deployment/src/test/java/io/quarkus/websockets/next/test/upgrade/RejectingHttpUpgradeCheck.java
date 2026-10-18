package io.quarkus.websockets.next.test.upgrade;

import io.quarkus.websockets.next.HttpUpgradeCheck;
import io.smallrye.mutiny.Uni;

public class RejectingHttpUpgradeCheck implements HttpUpgradeCheck {

    static final String REJECT_HEADER = "reject";

    @Override
    public Uni<CheckResult> perform(HttpUpgradeContext context) {
        if (shouldCheckUpgrade(context)) {
            return CheckResult.rejectUpgrade(403);
        }
        return CheckResult.permitUpgrade();
    }

    protected boolean shouldCheckUpgrade(HttpUpgradeContext context) {
        return context.httpRequest().headers().contains(REJECT_HEADER);
    }

}
