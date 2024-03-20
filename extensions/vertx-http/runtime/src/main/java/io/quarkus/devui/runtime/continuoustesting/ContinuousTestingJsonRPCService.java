package io.quarkus.devui.runtime.continuoustesting;

import java.util.function.Consumer;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.dev.testing.ContinuousTestingSharedStateManager;
import io.quarkus.vertx.http.runtime.devmode.Json;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;

@ApplicationScoped
public class ContinuousTestingJsonRPCService implements Consumer<ContinuousTestingSharedStateManager.State> {

    private final BroadcastProcessor<String> stateBroadcaster = BroadcastProcessor.create();
    private final BroadcastProcessor<Object> resultBroadcaster = BroadcastProcessor.create();

    private String lastKnownState = "";
    private Object lastKnownResults = "";

    @Override
    public void accept(ContinuousTestingSharedStateManager.State state) {
        Json.JsonObjectBuilder response = Json.object();
        response.put("running", state.running);
        response.put("inProgress", state.inProgress);
        response.put("run", state.run);
        response.put("passed", state.passed);
        response.put("failed", state.failed);
        response.put("skipped", state.skipped);
        response.put("isBrokenOnly", state.isBrokenOnly);
        response.put("isTestOutput", state.isTestOutput);
        response.put("isInstrumentationBasedReload", state.isInstrumentationBasedReload);
        response.put("isLiveReload", state.isLiveReload);
        this.lastKnownState = response.build();
        stateBroadcaster.onNext(this.lastKnownState);
        this.lastKnownResults = DevConsoleManager.invoke("devui-continuous-testing.getResults");
        if (this.lastKnownResults != null) {
            resultBroadcaster.onNext(this.lastKnownResults);
        }
    }

    public Multi<String> streamTestState() {
        return stateBroadcaster;
    }

    public Multi<Object> streamTestResults() {
        return resultBroadcaster;
    }

    @NonBlocking
    public String lastKnownState() {
        return this.lastKnownState;
    }

    @NonBlocking
    public Object lastKnownResults() {
        return this.lastKnownResults;
    }
}
