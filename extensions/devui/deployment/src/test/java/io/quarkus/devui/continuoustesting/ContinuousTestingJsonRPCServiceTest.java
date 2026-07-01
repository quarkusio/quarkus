package io.quarkus.devui.continuoustesting;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.dev.testing.ContinuousTestingSharedStateManager;
import io.quarkus.dev.testing.results.TestRunResultsInterface;
import io.quarkus.devui.runtime.continuoustesting.ContinuousTestingJsonRPCService;

public class ContinuousTestingJsonRPCServiceTest {

    @AfterEach
    void cleanup() {
        DevConsoleManager.close();
    }

    @Test
    void acceptShouldNotThrowWhenActionNotRegistered() {
        ContinuousTestingJsonRPCService service = new ContinuousTestingJsonRPCService();
        ContinuousTestingSharedStateManager.State state = new ContinuousTestingSharedStateManager.StateBuilder()
                .setLastRun(1)
                .setRunning(true)
                .setPassed(5)
                .setFailed(1)
                .setSkipped(0)
                .build();

        service.accept(state);

        assertThat(service.currentState()).isNotNull();
        assertThat(service.currentState().getResult()).isNotNull();
        assertThat(service.currentState().getResult().getTotalTime()).isEqualTo(0L);
        assertThat(service.currentState().getResult().getPassed()).isEmpty();
        assertThat(service.currentState().getResult().getFailed()).isEmpty();
        assertThat(service.currentState().getResult().getSkipped()).isEmpty();
    }

    @Test
    void acceptShouldWorkWhenActionReturnsNull() {
        DevConsoleManager.register("devui-continuous-testing_getResults",
                (Function<Map<String, String>, TestRunResultsInterface>) ignored -> null);

        ContinuousTestingJsonRPCService service = new ContinuousTestingJsonRPCService();
        ContinuousTestingSharedStateManager.State state = new ContinuousTestingSharedStateManager.StateBuilder()
                .setLastRun(1)
                .setRunning(true)
                .setPassed(3)
                .setFailed(0)
                .setSkipped(2)
                .build();

        service.accept(state);

        assertThat(service.currentState()).isNotNull();
        assertThat(service.currentState().getResult().getTotalTime()).isEqualTo(0L);
        assertThat(service.currentState().getConfig().isEnabled()).isTrue();
        assertThat(service.currentState().getResult().getCounts().getPassed()).isEqualTo(3);
        assertThat(service.currentState().getResult().getCounts().getSkipped()).isEqualTo(2);
    }
}