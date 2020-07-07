package io.quarkus.jberet.deployment;

import static javax.batch.runtime.Metric.MetricType.COMMIT_COUNT;
import static javax.batch.runtime.Metric.MetricType.READ_COUNT;
import static javax.batch.runtime.Metric.MetricType.WRITE_COUNT;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.AbstractItemReader;
import javax.batch.api.chunk.AbstractItemWriter;
import javax.batch.api.chunk.ItemProcessor;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.Metric;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ChunkPartitionTest {
    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(BatchTestUtils.class, PartitionItemReader.class, PartitionItemProcessor.class,
                            PartitionItemWriter.class)
                    .addAsManifestResource("partition.xml", "batch-jobs/partition.xml"));

    @Test
    void partition() {
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        long executionId = jobOperator.start("partition", new Properties());

        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            JobExecution jobExecution = jobOperator.getJobExecution(executionId);
            return BatchStatus.COMPLETED.equals(jobExecution.getBatchStatus());
        });

        jobOperator.getStepExecutions(executionId).stream().findFirst().ifPresent(
                stepExecution -> {
                    Map<Metric.MetricType, Long> metricsMap = BatchTestUtils.getMetricsMap(stepExecution.getMetrics());
                    assertEquals(20L, metricsMap.get(READ_COUNT).longValue());
                    assertEquals(10L, metricsMap.get(WRITE_COUNT).longValue());
                    assertEquals((10L / 3 + (10 % 3 > 0 ? 1 : 0)) * 2, metricsMap.get(COMMIT_COUNT).longValue());
                });
    }

    @Named
    @Dependent
    public static class PartitionItemReader extends AbstractItemReader {
        @Inject
        @BatchProperty(name = "start")
        int start;
        @Inject
        @BatchProperty(name = "end")
        int end;

        private PrimitiveIterator.OfInt intStream;

        @Override
        public void open(Serializable e) {
            intStream = IntStream.rangeClosed(start, end).iterator();
        }

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
    public static class PartitionItemProcessor implements ItemProcessor {
        @Override
        public Object processItem(Object t) {
            return (int) t % 2 == 0 ? null : t;
        }
    }

    @Named
    @Dependent
    public static class PartitionItemWriter extends AbstractItemWriter {
        @Override
        public void writeItems(List list) {
        }
    }
}
