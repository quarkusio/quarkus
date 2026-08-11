package io.quarkus.container.image.jib.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import com.google.cloud.tools.jib.event.events.ProgressEvent;
import com.google.cloud.tools.jib.event.progress.Allocation;
import com.google.cloud.tools.jib.event.progress.ProgressEventHandler;

/**
 * The clock is injected, so the throttling rules are asserted without sleeping and none of these tests depend on wall
 * clock time.
 */
class JibProgressReporterTest {

    private final List<String> messages = new ArrayList<>();
    private final AtomicLong nanos = new AtomicLong();
    private final JibProgressReporter reporter = new JibProgressReporter(messages::add, nanos::get);

    private void advance(Duration duration) {
        nanos.addAndGet(duration.toNanos());
    }

    @Test
    void logsNothingForABuildThatFinishesQuickly() {
        // a local Docker daemon build, or a push whose layers all already exist in the registry
        for (int i = 0; i <= 10; i++) {
            advance(Duration.ofMillis(100));
            reporter.report(i / 10.0);
        }

        assertThat(messages).isEmpty();
    }

    @Test
    void reportsProgressOncePastTheFirstInterval() {
        advance(Duration.ofSeconds(5));
        reporter.report(0.42);

        assertThat(messages).containsExactly("Container image build progress: 42% (5 s elapsed)");
    }

    @Test
    void throttlesByElapsedTime() {
        advance(Duration.ofSeconds(5));
        reporter.report(0.1);
        assertThat(messages).hasSize(1);

        // progress jumped far enough, but not enough time has passed since the previous line
        advance(Duration.ofSeconds(1));
        reporter.report(0.9);
        assertThat(messages).hasSize(1);

        advance(Duration.ofSeconds(4));
        reporter.report(0.9);
        assertThat(messages).hasSize(2)
                .last().isEqualTo("Container image build progress: 90% (10 s elapsed)");
    }

    @Test
    void throttlesByPercentageDelta() {
        advance(Duration.ofSeconds(5));
        reporter.report(0.10);
        assertThat(messages).hasSize(1);

        // plenty of time has passed, but 12% is only two percentage points on from the previous line
        advance(Duration.ofSeconds(60));
        reporter.report(0.12);
        assertThat(messages).hasSize(1);

        advance(Duration.ofSeconds(5));
        reporter.report(0.15);
        assertThat(messages).hasSize(2);
    }

    @Test
    void clampsOutOfRangeProgress() {
        advance(Duration.ofSeconds(5));
        reporter.report(1.4);

        assertThat(messages).containsExactly("Container image build progress: 100% (5 s elapsed)");
    }

    /**
     * Drives the reporter through the real jib types rather than calling {@link JibProgressReporter#report(double)}
     * directly, so that the wiring done in {@code JibProcessor.createContainerizer} is covered too.
     */
    @Test
    void isDrivenByRealProgressEvents() {
        ProgressEventHandler handler = new ProgressEventHandler(reporter);
        Allocation root = Allocation.newRoot("pushing blobs", 4);

        handler.accept(new ProgressEvent(root, 1));
        assertThat(messages).isEmpty(); // 25%, but the first interval has not elapsed yet

        advance(Duration.ofSeconds(5));
        handler.accept(new ProgressEvent(root, 1));

        assertThat(messages).containsExactly("Container image build progress: 50% (5 s elapsed)");
    }

}
