package io.quarkus.jberet.deployment;

import static org.awaitility.Awaitility.await;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.batch.api.AbstractBatchlet;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class BatchConfigSourceTest {
    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(ConfigBatchlet.class)
                    .addAsManifestResource("batchlet.xml", "batch-jobs/batchlet.xml"));

    @Named("batchlet")
    @Dependent
    public static class ConfigBatchlet extends AbstractBatchlet {
        @Inject
        @ConfigProperty(name = "name", defaultValue = "")
        String name;

        @Override
        public String process() {
            if ("naruto".equals(name)) {
                return BatchStatus.COMPLETED.toString();
            } else {
                return BatchStatus.FAILED.toString();
            }
        }
    }

    @Test
    public void configBatchlet() {
        Properties jobParameters = new Properties();
        jobParameters.setProperty("name", "naruto");
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        long executionId = jobOperator.start("batchlet", jobParameters);

        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            JobExecution jobExecution = jobOperator.getJobExecution(executionId);
            return BatchStatus.COMPLETED.equals(jobExecution.getBatchStatus());
        });
    }
}
