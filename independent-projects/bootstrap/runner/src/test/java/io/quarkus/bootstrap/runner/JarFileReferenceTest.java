package io.quarkus.bootstrap.runner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class JarFileReferenceTest {
    private static final Path MISSING_JAR = Path.of("/this/jar/does/not/exist.jar");

    /**
     * When opening the jar file fails, the thread that won the CAS must not leave its
     * incomplete future published in {@link JarResource#jarFileReference}. Such a future
     * never becomes complete, so every later caller parks on it forever.
     */
    @Test
    public void failedJarOpeningMustNotLeaveAnythingPublished() {
        JarResource jarResource = new JarResource(null, MISSING_JAR);

        assertThatThrownBy(() -> jarResource.getResourceData("whatever"))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(IOException.class);

        // A failed load must not leave a trace, neither a pending future (every later
        // caller would park forever) nor a failed one (the fast path in `withJarFile`
        // joins it, so every later caller would keep throwing, even if the jar file
        // becomes readable in the meantime). Only `null` lets the next caller retry.
        assertThat(jarResource.jarFileReference.get()).isNull();
    }

    /**
     * The observable consequence of the above: a second caller must get the same prompt
     * failure as the first one instead of hanging.
     * <p>
     * The call runs on a daemon thread on purpose. If the bug is present, that thread
     * parks in an uninterruptible {@link java.util.concurrent.CompletableFuture#join()}
     * and can never be reclaimed; being a daemon, it at least does not prevent JVM exit
     * and the test reports a failure instead of hanging the build.
     */
    @Test
    public void secondCallerMustFailRatherThanPark() throws Exception {
        JarResource jarResource = new JarResource(null, MISSING_JAR);

        assertThatThrownBy(() -> jarResource.getResourceData("whatever"))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(IOException.class);

        ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "second-caller");
            thread.setDaemon(true);
            return thread;
        });
        try {
            Future<byte[]> secondCall = executor.submit(() -> jarResource.getResourceData("whatever"));
            assertThatThrownBy(() -> secondCall.get(10, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .cause()
                    .isInstanceOf(RuntimeException.class)
                    .hasCauseInstanceOf(IOException.class);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * The fast path of {@link JarFileReference#withJarFile} reads the published future into a local
     * variable and only then looks at it. The loading thread can fail in between: it unpublishes its
     * future first and completes it exceptionally second, so the local reference is left pointing at
     * a future that is no longer published and has just failed. Joining it must not propagate a
     * {@code CompletionException} to the caller, the load has to be retried instead.
     */
    @Test
    public void futureFailingWhileObservedByFastPathMustBeRetried(@TempDir Path tmp) throws Exception {
        JarResource jarResource = new JarResource(null, createJarFile(tmp));
        jarResource.jarFileReference.set(new FailingWhenObserved(jarResource, Trigger.IS_DONE));

        // The jar file itself is perfectly readable, so the retry must succeed
        assertThat(jarResource.getResourceData("hello.txt")).asString().isEqualTo("hello");
        assertThat(jarResource.jarFileReference.get()).isNotNull();
        assertThat(jarResource.jarFileReference.get()).isNotInstanceOf(FailingWhenObserved.class);
    }

    /**
     * The same, one step later: a thread that lost the race to load the jar file reads the published
     * future and parks on it, and the loading thread then fails. The join throws rather than
     * returning, which must lead to a retry and not to a failure reported to the caller.
     */
    @Test
    public void futureFailingWhileJoinedMustBeRetried(@TempDir Path tmp) throws Exception {
        JarResource jarResource = new JarResource(null, createJarFile(tmp));
        jarResource.jarFileReference.set(new FailingWhenObserved(jarResource, Trigger.JOIN));

        // The jar file itself is perfectly readable, so the retry must succeed
        assertThat(jarResource.getResourceData("hello.txt")).asString().isEqualTo("hello");
        assertThat(jarResource.jarFileReference.get()).isNotNull();
        assertThat(jarResource.jarFileReference.get()).isNotInstanceOf(FailingWhenObserved.class);
    }

    /**
     * {@link JarResource#close()} reads the future into a local the same way and then calls {@code getNow},
     * which throws just like {@code join} on a future that failed in the meantime. There is nothing to
     * close in that case, and closing must not fail.
     */
    @Test
    public void closeMustTolerateAFutureFailingWhileObserved(@TempDir Path tmp) throws Exception {
        JarResource jarResource = new JarResource(null, createJarFile(tmp));
        jarResource.jarFileReference.set(new FailingWhenObserved(jarResource, Trigger.GET_NOW));

        assertThatCode(jarResource::close).doesNotThrowAnyException();
    }

    private enum Trigger {
        IS_DONE,
        JOIN,
        GET_NOW
    }

    /**
     * A future that simulates the loading thread failing at the exact moment this future is observed,
     * in the same order {@link JarFileReference} does it: unpublish first, complete exceptionally
     * second. Only the timing of the failure is injected; the observed methods still delegate to
     * {@link CompletableFuture}, so it is the real {@code CompletionException} that reaches the code
     * under test.
     */
    private static final class FailingWhenObserved extends CompletableFuture<JarFileReference> {
        private final JarResource jarResource;
        private final Trigger trigger;

        FailingWhenObserved(JarResource jarResource, Trigger trigger) {
            this.jarResource = jarResource;
            this.trigger = trigger;
        }

        private void simulateLoaderFailure(Trigger observed) {
            if (observed == trigger) {
                jarResource.jarFileReference.compareAndSet(this, null);
                completeExceptionally(new RuntimeException("Failed to open " + jarResource.jarPath));
            }
        }

        @Override
        public boolean isDone() {
            simulateLoaderFailure(Trigger.IS_DONE);
            return super.isDone();
        }

        @Override
        public JarFileReference join() {
            simulateLoaderFailure(Trigger.JOIN);
            return super.join();
        }

        @Override
        public JarFileReference getNow(JarFileReference valueIfAbsent) {
            simulateLoaderFailure(Trigger.GET_NOW);
            return super.getNow(valueIfAbsent);
        }
    }

    private static Path createJarFile(Path tmp) {
        Path jarFile = tmp.resolve("test.jar");
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class);
        jar.add(new StringAsset("hello"), "hello.txt");
        jar.as(ZipExporter.class).exportTo(jarFile.toFile(), true);
        return jarFile;
    }
}
