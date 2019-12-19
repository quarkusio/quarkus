package io.quarkus.jberet.deployment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;

import org.jberet.creation.BatchBeanProducer;
import org.jberet.job.model.Job;
import org.jberet.job.model.JobParser;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.jberet.runtime.JBeretRecorder;
import io.quarkus.jberet.runtime.QuarkusJobOperator;

public class JBeretProcessor {

    private static final Logger log = Logger.getLogger("io.quarkus.jberet");

    @BuildStep
    public void registerExtension(BuildProducer<FeatureBuildItem> feature,
            BuildProducer<CapabilityBuildItem> capability) {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.JBERET));
        capability.produce(new CapabilityBuildItem(Capabilities.JBERET));
    }

    @BuildStep
    public ServiceProviderBuildItem jobOperatorServiceProvider() {
        return new ServiceProviderBuildItem("javax.batch.operations.JobOperator",
                "org.jberet.operations.DelegatingJobOperator");
    }

    /**
     * Prevent JobOperatorContext$DefaultHolder from eagerly initializing because it depends on a ServiceLoader
     * entry for the BatchRuntime, which we don't use. With this trigger turned off, it won't ever be initialized.
     */
    @BuildStep
    public RuntimeInitializedClassBuildItem runtimeInitializedDefaultHolder() {
        return new RuntimeInitializedClassBuildItem("org.jberet.spi.JobOperatorContext$DefaultHolder");
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void initializeEnvironment(JBeretRecorder recorder,
            BuildProducer<HotDeploymentWatchedFileBuildItem> watchedFiles)
            throws IOException, URISyntaxException, XMLStreamException {
        QuarkusJobOperator operator = recorder.createJobOperator();

        // pre-parse job definitions from xml files
        URL directoryUrl = Thread.currentThread().getContextClassLoader().getResource("META-INF/batch-jobs");
        if (directoryUrl != null) {
            File batchJobsDirectory = new File(directoryUrl.toURI());
            for (File batchJobXmlFile : batchJobsDirectory.listFiles(((dir, name) -> name.endsWith(".xml")))) {
                InputStream fileContents = Files.newInputStream(batchJobXmlFile.toPath());
                Job job = JobParser.parseJob(fileContents, Thread.currentThread().getContextClassLoader(), new XMLResolver() {
                    @Override
                    public Object resolveEntity(final String publicID, final String systemID, final String baseURI,
                            final String namespace) throws XMLStreamException {
                        return null;
                        // TODO implement this properly. For inspiration see WildFlyJobXmlResolver:283
                    }
                });
                String xmlName = batchJobXmlFile.getName().substring(0, batchJobXmlFile.getName().length() - 4);
                job.setJobXmlName(xmlName);
                recorder.jobDefinition(operator, job);
                watchedFiles.produce(new HotDeploymentWatchedFileBuildItem("META-INF/batch-jobs/" + batchJobXmlFile.getName()));
                log.debug("Processed job with ID " + job.getId() + "  from file " + batchJobXmlFile);
            }
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void batchExecutor(JBeretRecorder recorder,
            JBeretBuildTimeConfig config,
            ShutdownContextBuildItem shutdownContext) {
        recorder.createExecutor(config.maximumPoolSize, shutdownContext);
    }

    @BuildStep
    public List<AdditionalBeanBuildItem> additionalBeans() {
        List<AdditionalBeanBuildItem> beans = new ArrayList<>();
        beans.add(new AdditionalBeanBuildItem(BatchBeanProducer.class));
        return beans;
    }

    // TODO: add @Dependent to detected batch components without CDI scope

}
