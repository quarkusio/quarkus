package io.quarkus.jberet.runtime;

import java.util.Properties;

import javax.batch.operations.JobSecurityException;
import javax.batch.operations.JobStartException;
import javax.batch.operations.NoSuchJobException;
import javax.transaction.TransactionManager;

import org.jberet.job.model.Job;
import org.jberet.operations.AbstractJobOperator;
import org.jberet.repository.InMemoryRepository;
import org.jberet.repository.JobRepository;
import org.jberet.spi.ArtifactFactory;
import org.jberet.spi.BatchEnvironment;
import org.jberet.spi.JobExecutor;
import org.jberet.spi.JobXmlResolver;

public class QuarkusJobOperator extends AbstractJobOperator {

    private BatchEnvironment batchEnvironment;
    private JobXmlResolver jobXmlResolver;
    private JobRepository jobRepository;
    private TransactionManager transactionManager;
    private ArtifactFactory artifactFactory;
    private JobExecutor jobExecutor;
    private JobDefinitionRepository jobDefinitionRepository;

    public QuarkusJobOperator() {
        jobXmlResolver = new QuarkusJobXmlResolver();
        jobRepository = new InMemoryRepository(); // TODO support other repository types depending on config
        transactionManager = com.arjuna.ats.jta.TransactionManager.transactionManager();
        artifactFactory = new QuarkusArtifactFactory();
        jobDefinitionRepository = new JobDefinitionRepository();
    }

    // the part of the initialization that has to be done in RUNTIME_INIT
    public void initialize() {
        jobExecutor = new QuarkusJobExecutor(JBeretExecutorHolder.get());
        batchEnvironment = new QuarkusBatchEnvironment(jobXmlResolver, jobRepository, transactionManager, artifactFactory,
                jobExecutor);
    }

    @Override
    public long start(String jobXMLName, Properties jobParameters) throws JobStartException, JobSecurityException {
        // for now, we assume that all job XML files were identified and parsed during build, and now the job
        // definitions are available in the JobDefinitionRepository
        Job jobDefinition = jobDefinitionRepository.getJobDefinition(jobXMLName);
        if (jobDefinition != null) {
            return super.start(jobDefinition, jobParameters);
        } else {
            throw new NoSuchJobException("Job with xml name " + jobXMLName + " was not found");
        }
    }

    @Override
    public BatchEnvironment getBatchEnvironment() {
        return batchEnvironment;
    }

    public JobDefinitionRepository getJobDefinitionRepository() {
        return jobDefinitionRepository;
    }

}
