package io.quarkus.container.image.jib.deployment;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

import org.jboss.logging.Logger;

import com.google.cloud.tools.jib.event.progress.ProgressEventHandler;

/**
 * Logs the progress of a jib container image build. This matters most when the image is pushed to a registry, which
 * otherwise produces no output whatsoever between the start of the build and the final "Pushed container image" line,
 * and which can take several minutes on a slow connection or when more than one platform is built.
 * <p>
 * Jib reports progress through very frequent events - {@link ProgressEventHandler} invokes its notifier on every
 * change - and the augmentation log is not a terminal, so it cannot be redrawn in place the way
 * {@code jib-maven-plugin} redraws its progress bar. Output is therefore throttled to occasional discrete lines: a
 * line is logged only once both {@link #MINIMUM_INTERVAL} has elapsed since the previous line and progress has
 * advanced by at least {@link #MINIMUM_PERCENTAGE_DELTA}. A build that finishes quickly - a local Docker daemon build,
 * or a push whose layers all already exist in the registry - therefore logs nothing at all.
 */
final class JibProgressReporter implements Consumer<ProgressEventHandler.Update> {

    private static final Duration MINIMUM_INTERVAL = Duration.ofSeconds(5);
    private static final int MINIMUM_PERCENTAGE_DELTA = 5;

    private final Consumer<String> messageConsumer;
    private final LongSupplier nanoTimeSupplier;
    private final long startNanos;

    private long lastReportNanos;
    private int lastReportedPercentage;

    JibProgressReporter(Logger log) {
        this(log::info, System::nanoTime);
    }

    JibProgressReporter(Consumer<String> messageConsumer, LongSupplier nanoTimeSupplier) {
        this.messageConsumer = messageConsumer;
        this.nanoTimeSupplier = nanoTimeSupplier;
        this.startNanos = nanoTimeSupplier.getAsLong();
        this.lastReportNanos = startNanos;
    }

    @Override
    public void accept(ProgressEventHandler.Update update) {
        report(update.getProgress());
    }

    /**
     * Called from jib's own threads, potentially concurrently, hence the synchronization. The body only reads a clock
     * and compares a couple of numbers, so it is not worth avoiding the lock; jib additionally throttles the
     * underlying per-blob progress before it ever reaches here.
     */
    synchronized void report(double progress) {
        int percentage = toPercentage(progress);
        long now = nanoTimeSupplier.getAsLong();
        if (percentage - lastReportedPercentage < MINIMUM_PERCENTAGE_DELTA
                || now - lastReportNanos < MINIMUM_INTERVAL.toNanos()) {
            return;
        }
        lastReportNanos = now;
        lastReportedPercentage = percentage;

        long elapsedSeconds = Duration.ofNanos(now - startNanos).toSeconds();
        messageConsumer.accept(
                String.format("Container image build progress: %d%% (%d s elapsed)", percentage, elapsedSeconds));
    }

    private static int toPercentage(double progress) {
        // jib documents 1.0 as fully complete, but clamp anyway so that rounding can never produce a nonsensical value
        return Math.clamp((int) Math.round(progress * 100), 0, 100);
    }

}
