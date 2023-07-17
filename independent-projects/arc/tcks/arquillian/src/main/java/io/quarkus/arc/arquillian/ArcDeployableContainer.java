package io.quarkus.arc.arquillian;

import java.io.File;
import java.io.IOException;

import jakarta.enterprise.event.Shutdown;
import jakarta.enterprise.event.Startup;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.context.annotation.DeploymentScoped;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.ArcInitConfig;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.arquillian.utils.ClassLoading;
import io.quarkus.arc.arquillian.utils.Directories;

public class ArcDeployableContainer implements DeployableContainer<ArcContainerConfiguration> {
    @Inject
    @DeploymentScoped
    private InstanceProducer<DeploymentDir> deploymentDir;

    @Inject
    @DeploymentScoped
    private InstanceProducer<DeploymentClassLoader> deploymentClassLoader;

    @Inject
    @DeploymentScoped
    private InstanceProducer<ArcContainer> runningArc;

    @Inject
    private Instance<TestClass> testClass;

    static Object testInstance;

    @Override
    public Class<ArcContainerConfiguration> getConfigurationClass() {
        return ArcContainerConfiguration.class;
    }

    @Override
    public void setup(ArcContainerConfiguration configuration) {
    }

    @Override
    public ProtocolDescription getDefaultProtocol() {
        return new ProtocolDescription("ArC");
    }

    @Override
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        if (System.getProperty("saveArchive") != null) {
            File file = new File(archive.getName());
            archive.as(ZipExporter.class).exportTo(file);
            System.out.println("Archive for test " + testClass.get().getName() + " saved in: " + file.getAbsolutePath());
        }

        if (testClass.get() == null) {
            throw new IllegalStateException("Test class not available");
        }
        String testClassName = testClass.get().getName();

        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            DeploymentDir deploymentDir = new DeploymentDir();
            this.deploymentDir.set(deploymentDir);

            DeploymentClassLoader deploymentClassLoader = new Deployer(archive, deploymentDir, testClassName).deploy();
            this.deploymentClassLoader.set(deploymentClassLoader);

            Thread.currentThread().setContextClassLoader(deploymentClassLoader);

            // passing strict mode here allows it to be visible in runtime
            ArcContainer arcContainer = Arc.initialize(ArcInitConfig.builder().setStrictCompatibility(true).build());
            runningArc.set(arcContainer);
            arcContainer.beanManager().getEvent().fire(new Startup());

            Class<?> actualTestClass = Class.forName(testClassName, true, deploymentClassLoader);
            testInstance = findTest(arcContainer, actualTestClass);
        } catch (Throwable t) {
            // clone the exception into the correct class loader
            Throwable nt = ClassLoading.cloneExceptionIntoSystemCL(t);
            throw new DeploymentException("Unable to start ArC", nt);
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }

        return new ProtocolMetaData();
    }

    private Object findTest(ArcContainer arc, Class<?> testClass) {
        InjectableInstance<?> instance = arc.select(testClass);
        if (instance.isResolvable()) {
            return instance.get();
        }

        if (testClass.getTypeParameters().length > 0) {
            // fallback for generic test classes, whose set of bean types does not contain a `Class`
            // but a `ParameterizedType` instead
            for (InstanceHandle<Object> handle : arc.listAll(Object.class)) {
                if (testClass.equals(handle.getBean().getBeanClass())) {
                    return handle.get();
                }
            }
        }

        throw new IllegalStateException("No bean: " + testClass);
    }

    @Override
    public void undeploy(Archive<?> archive) throws DeploymentException {
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            DeploymentClassLoader deploymentClassLoader = this.deploymentClassLoader.get();

            ArcContainer arcContainer = runningArc.get();
            if (arcContainer != null) {
                Thread.currentThread().setContextClassLoader(deploymentClassLoader);
                arcContainer.beanManager().getEvent().fire(new Shutdown());
                Arc.shutdown();
            }
            testInstance = null;

            try {
                deploymentClassLoader.close();
            } catch (IOException e) {
                throw new DeploymentException("Failed to close deployment classloader", e);
            }

            DeploymentDir deploymentDir = this.deploymentDir.get();
            if (deploymentDir != null) {
                if (System.getProperty("retainDeployment") == null) {
                    Directories.deleteDirectory(deploymentDir.root);
                } else {
                    System.out.println("Deployment for test " + testClass.get().getName()
                            + " retained in: " + deploymentDir.root);
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    @Override
    public void deploy(Descriptor descriptor) {
        throw new UnsupportedOperationException();

    }

    @Override
    public void undeploy(Descriptor descriptor) {
        throw new UnsupportedOperationException();

    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }
}
