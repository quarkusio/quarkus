package io.quarkus.jberet.deployment;

import static java.util.stream.Collectors.toList;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.batch.api.AbstractBatchlet;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.StepExecution;
import javax.enterprise.context.Dependent;
import javax.inject.Named;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class SplitTest {
    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(SplitBatchletFirst.class, SplitBatchletSecond.class, SplitBatchletThird.class)
                    .addAsManifestResource("split.xml", "batch-jobs/split.xml"));

    @Test
    void split() {
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        long executionId = jobOperator.start("split", new Properties());

        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            JobExecution jobExecution = jobOperator.getJobExecution(executionId);
            return BatchStatus.COMPLETED.equals(jobExecution.getBatchStatus());
        });

        List<StepExecution> stepExecutions = jobOperator.getStepExecutions(executionId);
        List<String> steps = stepExecutions.stream().map(StepExecution::getStepName).collect(toList());

        assertEquals(3, stepExecutions.size());
        assertTrue(steps.contains("split-step-1"));
        assertTrue(steps.contains("split-step-2"));
        assertTrue(steps.contains("split-step-3"));

        // Steps 'step1' and 'step2' can appear in any order, since they were executed in parallel.
        assertTrue(steps.get(0).equals("split-step-1") || steps.get(0).equals("split-step-2"));
        assertTrue(steps.get(1).equals("split-step-1") || steps.get(1).equals("split-step-2"));
        assertEquals("split-step-3", steps.get(2));
    }

    @Named
    @Dependent
    public static class SplitBatchletFirst extends AbstractBatchlet {
        @Override
        public String process() {
            return BatchStatus.COMPLETED.toString();
        }
    }

    @Named
    @Dependent
    public static class SplitBatchletSecond extends AbstractBatchlet {
        @Override
        public String process() {
            return BatchStatus.COMPLETED.toString();
        }
    }

    @Named
    @Dependent
    public static class SplitBatchletThird extends AbstractBatchlet {
        @Override
        public String process() {
            return BatchStatus.COMPLETED.toString();
        }
    }
}
