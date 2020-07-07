package io.quarkus.jberet.runtime;

import java.util.List;

import javax.transaction.TransactionManager;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.jberet.job.model.Job;
import org.jberet.spi.JobOperatorContext;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class JBeretRecorder {
    public void registerJobs(List<Job> jobs) {
        JBeretDataHolder.registerJobs(jobs);
    }

    public void initJobOperator(BeanContainer beanContainer) {
        ManagedExecutor managedExecutor = beanContainer.instance(ManagedExecutor.class);
        TransactionManager transactionManager = beanContainer.instance(TransactionManager.class);

        QuarkusJobOperator operator = new QuarkusJobOperator(managedExecutor, transactionManager, JBeretDataHolder.getJobs());
        JobOperatorContext operatorContext = JobOperatorContext.create(operator);
        JobOperatorContext.setJobOperatorContextSelector(() -> operatorContext);
    }
}
