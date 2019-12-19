package io.quarkus.jberet.runtime;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jberet.job.model.Job;
import org.jberet.spi.JobOperatorContext;
import org.jboss.logging.Logger;

import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class JBeretRecorder {

    private static final Logger log = Logger.getLogger("io.quarkus.jberet");

    public QuarkusJobOperator createJobOperator() {
        QuarkusJobOperator operator = new QuarkusJobOperator();
        JobOperatorContext operatorContext = JobOperatorContext.create(operator);
        JobOperatorContext.setJobOperatorContextSelector(() -> operatorContext);
        return operator;
    }

    public void jobDefinition(QuarkusJobOperator operator, Job job) {
        operator.getJobDefinitionRepository().addJobDefinition(job.getJobXmlName(), job);
    }

    public void createExecutor(Integer maximumPoolSize, ShutdownContext shutdownContext) {
        // FIXME: how to properly create the executor?
        ExecutorService executor = Executors.newFixedThreadPool(maximumPoolSize);
        JBeretExecutorHolder.set(executor);
        shutdownContext.addShutdownTask(executor::shutdownNow);
        ((QuarkusJobOperator) JobOperatorContext.getJobOperatorContext().getJobOperator()).initialize();
    }
}
