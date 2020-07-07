package io.quarkus.jberet.deployment;

import static javax.batch.runtime.Metric.MetricType.COMMIT_COUNT;
import static javax.batch.runtime.Metric.MetricType.READ_COUNT;
import static javax.batch.runtime.Metric.MetricType.WRITE_COUNT;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import javax.batch.api.chunk.AbstractItemReader;
import javax.batch.api.chunk.AbstractItemWriter;
import javax.batch.api.chunk.ItemProcessor;
import javax.batch.api.chunk.listener.AbstractChunkListener;
import javax.batch.api.chunk.listener.AbstractItemProcessListener;
import javax.batch.api.chunk.listener.AbstractItemReadListener;
import javax.batch.api.chunk.listener.AbstractItemWriteListener;
import javax.batch.api.listener.AbstractJobListener;
import javax.batch.api.listener.AbstractStepListener;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.Metric;
import javax.enterprise.context.Dependent;
import javax.inject.Named;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class BatchListenersTest {
    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(BatchTestUtils.class,
                            BatchListeners.class,
                            JobListener.class,
                            StepListener.class,
                            ChunkListener.class,
                            ItemReadListener.class,
                            ItemProcessListener.class,
                            ItemWriteListener.class,
                            ListenerItemReader.class,
                            ListenerItemProcessor.class,
                            ListenerItemWriter.class)
                    .addAsManifestResource("listeners.xml", "batch-jobs/listeners.xml"));

    @Test
    void listeners() throws Exception {
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        Long executionId = jobOperator.start("listeners", new Properties());

        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            JobExecution jobExecution = jobOperator.getJobExecution(executionId);
            return BatchStatus.COMPLETED.equals(jobExecution.getBatchStatus());
        });

        assertTrue(BatchListeners.countDownLatch.await(0, TimeUnit.SECONDS));

        jobOperator.getStepExecutions(executionId).stream().findFirst().ifPresent(stepExecution -> {
            Map<Metric.MetricType, Long> metricsMap = BatchTestUtils.getMetricsMap(stepExecution.getMetrics());

            assertEquals(10L, metricsMap.get(READ_COUNT).longValue());
            assertEquals(10L / 2L, metricsMap.get(WRITE_COUNT).longValue());
            assertEquals(10L / 3 + (10L % 3 > 0 ? 1 : 0), metricsMap.get(COMMIT_COUNT).longValue());
        });
    }

    public static class BatchListeners {
        public static CountDownLatch countDownLatch = new CountDownLatch(60);
    }

    @Named
    @Dependent
    public static class JobListener extends AbstractJobListener {
        @Override
        public void beforeJob() {
            BatchListeners.countDownLatch.countDown();
        }

        @Override
        public void afterJob() {
            BatchListeners.countDownLatch.countDown();
        }
    }

    @Named
    @Dependent
    public static class StepListener extends AbstractStepListener {
        @Override
        public void beforeStep() {
            BatchListeners.countDownLatch.countDown();
        }

        @Override
        public void afterStep() {
            BatchListeners.countDownLatch.countDown();
        }
    }

    @Named
    @Dependent
    public static class ChunkListener extends AbstractChunkListener {
        @Override
        public void beforeChunk() {
            BatchListeners.countDownLatch.countDown();
        }

        @Override
        public void afterChunk() {
            BatchListeners.countDownLatch.countDown();
        }
    }

    @Named
    @Dependent
    public static class ItemReadListener extends AbstractItemReadListener {
        @Override
        public void beforeRead() {
            BatchListeners.countDownLatch.countDown();
        }

        @Override
        public void afterRead(Object item) {
            BatchListeners.countDownLatch.countDown();
        }

        @Override
        public void onReadError(Exception ex) {
            BatchListeners.countDownLatch.countDown();
        }
    }

    @Named
    @Dependent
    public static class ItemProcessListener extends AbstractItemProcessListener {
        @Override
        public void beforeProcess(Object item) {
            BatchListeners.countDownLatch.countDown();
        }

        @Override
        public void afterProcess(Object item, Object result) {
            BatchListeners.countDownLatch.countDown();
        }

        @Override
        public void onProcessError(Object item, Exception ex) {
            BatchListeners.countDownLatch.countDown();
        }
    }

    @Named
    @Dependent
    public static class ItemWriteListener extends AbstractItemWriteListener {
        @Override
        public void beforeWrite(List items) {
            BatchListeners.countDownLatch.countDown();
        }

        @Override
        public void afterWrite(List items) {
            BatchListeners.countDownLatch.countDown();
        }

        @Override
        public void onWriteError(List items, Exception ex) {
            BatchListeners.countDownLatch.countDown();
        }
    }

    @Named
    @Dependent
    public static class ListenerItemReader extends AbstractItemReader {
        private final PrimitiveIterator.OfInt intStream = IntStream.rangeClosed(1, 10).iterator();

        @Override
        public Object readItem() {
            if (intStream.hasNext()) {
                return intStream.next();
            }
            return null;
        }
    }

    @Named
    @Dependent
    public static class ListenerItemProcessor implements ItemProcessor {
        @Override
        public Object processItem(Object t) {
            return (int) t % 2 == 0 ? null : t;
        }
    }

    @Named
    @Dependent
    public static class ListenerItemWriter extends AbstractItemWriter {
        @Override
        public void writeItems(List list) {

        }
    }
}
