package io.quarkus.jberet.deployment;

import java.util.Properties;
import java.util.concurrent.TimeoutException;

import javax.batch.api.BatchProperty;
import javax.batch.api.Batchlet;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.jberet.deployment.helpers.JobHelper;
import io.quarkus.test.QuarkusUnitTest;

public class BatchletTest {

    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(DummyBatchlet.class)
                    .addAsManifestResource("dummy-batchlet.xml",
                            "batch-jobs/dummy-batchlet.xml"));

    @Named("dummyBatchlet")
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
            return null;
        }

        @Override
        public void stop() {
        }
    }

    @Test
    public void runBatchletJob() throws TimeoutException {
        Properties jobParameters = new Properties();
        jobParameters.setProperty("name", "david");
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        long executionId = jobOperator.start("dummy-batchlet", jobParameters);
        Assertions.assertEquals(BatchStatus.COMPLETED, JobHelper.waitForExecutionFinish(executionId));
    }

    @Test
    public void runBatchletJobWithUnexpectedParameter() throws TimeoutException {
        Properties jobParameters = new Properties();
        jobParameters.setProperty("name", "joe");

        JobOperator jobOperator = BatchRuntime.getJobOperator();
        long executionId = jobOperator.start("dummy-batchlet", jobParameters);
        Assertions.assertEquals(BatchStatus.FAILED, JobHelper.waitForExecutionFinish(executionId));
    }

}
