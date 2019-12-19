package io.quarkus.jberet.runtime;

import java.util.Properties;

import javax.transaction.TransactionManager;

import org.jberet.repository.JobRepository;
import org.jberet.spi.ArtifactFactory;
import org.jberet.spi.BatchEnvironment;
import org.jberet.spi.JobExecutor;
import org.jberet.spi.JobTask;
import org.jberet.spi.JobXmlResolver;

class QuarkusBatchEnvironment implements BatchEnvironment {

    private final JobXmlResolver jobXmlResolver;
    private final JobRepository jobRepository;
    private final TransactionManager transactionManager;
    private final ArtifactFactory artifactFactory;
    private final JobExecutor jobExecutor;

    private static final Properties PROPS = new Properties();

    public QuarkusBatchEnvironment(JobXmlResolver jobXmlResolver,
            JobRepository jobRepository,
            TransactionManager transactionManager,
            ArtifactFactory artifactFactory,
            JobExecutor jobExecutor) {
        this.jobXmlResolver = jobXmlResolver;
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.artifactFactory = artifactFactory;
        this.jobExecutor = jobExecutor;
    }

    @Override
    public ClassLoader getClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    @Override
    public ArtifactFactory getArtifactFactory() {
        return artifactFactory;
    }

    @Override
    public void submitTask(JobTask jobTask) {
        jobExecutor.execute(jobTask);
    }

    @Override
    public javax.transaction.TransactionManager getTransactionManager() {
        return transactionManager;
    }

    @Override
    public JobRepository getJobRepository() {
        return jobRepository;
    }

    @Override
    public JobXmlResolver getJobXmlResolver() {
        return jobXmlResolver;
    }

    @Override
    public Properties getBatchConfigurationProperties() {
        return PROPS;
    }
}
