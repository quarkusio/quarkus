package io.quarkus.devui.runtime.continuoustesting;

import java.util.Map;
import java.util.function.Consumer;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.dev.testing.ContinuousTestingSharedStateManager;
import io.quarkus.vertx.http.runtime.devmode.*;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;

@ApplicationScoped
public class ContinuousTestingJsonRPCService implements Consumer<ContinuousTestingSharedStateManager.State> {

    private final BroadcastProcessor<String> stateBroadcaster = BroadcastProcessor.create();
    private final BroadcastProcessor<String> resultBroadcaster = BroadcastProcessor.create();

    private String lastKnownState = "";
    private String lastKnownResults = "";

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
        this.lastKnownResults = this.getResults();
        if (this.lastKnownResults != null) {
            resultBroadcaster.onNext(this.lastKnownResults);
        }
    }

    public Multi<String> streamTestState() {
        return stateBroadcaster;
    }

    public Multi<String> streamTestResults() {
        return resultBroadcaster;
    }

    @NonBlocking
    public String lastKnownState() {
        return this.lastKnownState;
    }

    @NonBlocking
    public String lastKnownResults() {
        return this.lastKnownResults;
    }

    public boolean start() {
        return invokeAction("start");
    }

    @NonBlocking
    public boolean stop() {
        return invokeAction("stop");
    }

    @NonBlocking
    public boolean runAll() {
        return invokeAction("runAll");
    }

    @NonBlocking
    public boolean runFailed() {
        return invokeAction("runFailed");
    }

    @NonBlocking
    public boolean toggleBrokenOnly() {
        return invokeAction("toggleBrokenOnly");
    }

    public String getResults() {
        return invokeAction("getResults");
    }

    private <T> T invokeAction(String action) {
        try {
            return DevConsoleManager.invoke(NAMESPACE + DASH + action, Map.of());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final String NAMESPACE = "devui-continuous-testing";
    private static final String DASH = "-";
}