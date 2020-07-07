package io.quarkus.jberet.deployment;

import static org.awaitility.Awaitility.await;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.batch.api.BatchProperty;
import javax.batch.api.Batchlet;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import org.jberet.repository.JobRepository;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.StepExecutionImpl;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class BatchletTest {
    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(DummyBatchlet.class)
                    .addAsManifestResource("batchlet.xml", "batch-jobs/batchlet.xml"));

    @Named("batchlet")
    @Dependent
    public static class DummyBatchlet implements Batchlet {

        @Inject
        @BatchProperty(name = "name")
        String name;

        @Override
        public String process() {
            if (!name.equals("david")) {
                throw new RuntimeException("Unexpected value injected to 'name': " + name);
            }
            return BatchStatus.COMPLETED.toString();
        }

        @Override
        public void stop() {
        }
    }

    @Test
    public void runBatchletJob() {
        Properties jobParameters = new Properties();
        jobParameters.setProperty("name", "david");
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        long executionId = jobOperator.start("batchlet", jobParameters);

        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            JobExecution jobExecution = jobOperator.getJobExecution(executionId);
            return BatchStatus.COMPLETED.equals(jobExecution.getBatchStatus());
        });
    }

    @Test
    public void runBatchletJobWithUnexpectedParameter() {
        Properties jobParameters = new Properties();
        jobParameters.setProperty("name", "joe");

        JobOperator jobOperator = BatchRuntime.getJobOperator();
        long executionId = jobOperator.start("batchlet", jobParameters);

        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            JobExecution jobExecution = jobOperator.getJobExecution(executionId);
            return BatchStatus.FAILED.equals(jobExecution.getBatchStatus());
        });
    }

    @Inject
    JobOperator jobOperator;
    @Inject
    JobRepository jobRepository;

    @Test
    void start() {
        Properties jobParameters = new Properties();
        jobParameters.setProperty("name", "david");
        long executionId = jobOperator.start("batchlet", jobParameters);

        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            JobExecution jobExecution = jobOperator.getJobExecution(executionId);
            return BatchStatus.COMPLETED.equals(jobExecution.getBatchStatus());
        });
    }

    @Test
    void restart() {
        Properties jobParameters = new Properties();
        jobParameters.setProperty("name", "david");
        long executionId = jobOperator.start("batchlet", jobParameters);

        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            JobExecution jobExecution = jobOperator.getJobExecution(executionId);
            return BatchStatus.COMPLETED.equals(jobExecution.getBatchStatus());
        });

        final JobExecutionImpl jobExecuted = (JobExecutionImpl) jobRepository.getJobExecution(executionId);
        jobExecuted.setBatchStatus(BatchStatus.STOPPED);
        jobExecuted.getStepExecutions()
                .forEach(stepExecution -> ((StepExecutionImpl) stepExecution).setBatchStatus(BatchStatus.STOPPED));

        long restartId = jobOperator.restart(executionId, new Properties());
        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            JobExecution jobExecution = jobOperator.getJobExecution(restartId);
            return BatchStatus.COMPLETED.equals(jobExecution.getBatchStatus());
        });
    }
}
