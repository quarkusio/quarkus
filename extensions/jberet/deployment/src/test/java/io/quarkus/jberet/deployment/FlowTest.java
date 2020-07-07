package io.quarkus.jberet.deployment;

import static java.util.stream.Collectors.toList;
import static javax.batch.runtime.Metric.MetricType.COMMIT_COUNT;
import static javax.batch.runtime.Metric.MetricType.READ_COUNT;
import static javax.batch.runtime.Metric.MetricType.WRITE_COUNT;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import javax.batch.api.AbstractBatchlet;
import javax.batch.api.chunk.AbstractItemReader;
import javax.batch.api.chunk.AbstractItemWriter;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.Metric;
import javax.batch.runtime.StepExecution;
import javax.enterprise.context.Dependent;
import javax.inject.Named;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class FlowTest {
    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(FlowBatchletFirst.class, FlowBatchletSecond.class, FlowReader.class, FlowWriter.class,
                            BatchTestUtils.class)
                    .addAsManifestResource("flow.xml", "batch-jobs/flow.xml"));

    @Test
    void flow() {
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        long executionId = jobOperator.start("flow", new Properties());

        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            JobExecution jobExecution = jobOperator.getJobExecution(executionId);
            return BatchStatus.COMPLETED.equals(jobExecution.getBatchStatus());
        });

        List<StepExecution> stepExecutions = jobOperator.getStepExecutions(executionId);
        List<String> steps = stepExecutions.stream().map(StepExecution::getStepName).collect(toList());

        assertEquals(3, steps.size());
        assertArrayEquals(new String[] { "flow-step-1", "flow-step-2", "flow-step-3" }, steps.toArray());

        stepExecutions.stream().filter(step -> step.getStepName().equals("flow-step-2")).findFirst().ifPresent(
                stepExecution -> {
                    Map<Metric.MetricType, Long> metricsMap = BatchTestUtils.getMetricsMap(stepExecution.getMetrics());
                    assertEquals(5L, metricsMap.get(READ_COUNT).longValue());
                    assertEquals(5L, metricsMap.get(WRITE_COUNT).longValue());
                    assertEquals(5L / 3 + (5 % 3 > 0 ? 1 : 0), metricsMap.get(COMMIT_COUNT).longValue());
                });
    }

    @Named
    @Dependent
    public static class FlowBatchletFirst extends AbstractBatchlet {
        @Override
        public String process() {
            return BatchStatus.COMPLETED.toString();
        }
    }

    @Named
    @Dependent
    public static class FlowBatchletSecond extends AbstractBatchlet {
        @Override
        public String process() {
            return BatchStatus.COMPLETED.toString();
        }
    }

    @Named
    @Dependent
    public static class FlowReader extends AbstractItemReader {
        private PrimitiveIterator.OfInt intStream = IntStream.rangeClosed(1, 5).iterator();

        @Override
        public Object readItem() throws Exception {
            if (intStream.hasNext()) {
                return intStream.next();
            }
            return null;
        }
    }

    @Named
    @Dependent
    public static class FlowWriter extends AbstractItemWriter {
        @Override
        public void writeItems(final List<Object> items) throws Exception {

        }
    }
}
