package io.quarkus.arquillian;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.context.annotation.DeploymentScoped;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;

import io.quarkus.bootstrap.BootstrapClassLoaderFactory;
import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.util.PropertyUtils;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.builder.item.BuildItem;
import io.quarkus.runner.RuntimeRunner;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.test.common.PathTestHelper;
import io.quarkus.test.common.TestInstantiator;
import io.quarkus.test.common.http.TestHTTPResourceManager;

public class QuarkusDeployableContainer implements DeployableContainer<QuarkusConfiguration> {

    private static final Logger LOGGER = Logger.getLogger(QuarkusDeployableContainer.class);

    @Inject
    @DeploymentScoped
    private InstanceProducer<RuntimeRunner> runtimeRunner;

    @Inject
    @DeploymentScoped
    private InstanceProducer<Path> deploymentLocation;

    @Inject
    @DeploymentScoped
    private InstanceProducer<URLClassLoader> appClassloader;

    @Inject
    private Instance<TestClass> testClass;

    static Object testInstance;

    @Override
    public Class<QuarkusConfiguration> getConfigurationClass() {
        return QuarkusConfiguration.class;
    }

    @Override
    public void setup(QuarkusConfiguration configuration) {
        // No-op
    }

    @SuppressWarnings("rawtypes")
    @Override
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        if (testClass.get() == null) {
            throw new IllegalStateException("Test class not available");
        }
        Class testJavaClass = testClass.get().getJavaClass();

        try {
            // Export the test archive
            Path tmpLocation = Files.createTempDirectory("quarkus-arquillian-test");
            deploymentLocation.set(tmpLocation);
            archive.as(ExplodedExporter.class)
                    .exportExplodedInto(tmpLocation.toFile());
            Path appLocation;
            Set<Path> libraries = new HashSet<>();

            if (archive instanceof WebArchive) {
                // Quarkus does not support the WAR layout and so adapt the layout (similarly to quarkus-war-launcher)
                appLocation = tmpLocation.resolve("app").toAbsolutePath();
                //WEB-INF/lib -> lib/
                if (Files.exists(tmpLocation.resolve("WEB-INF/lib"))) {
                    Files.move(tmpLocation.resolve("WEB-INF/lib"), tmpLocation.resolve("lib"));
                }
                //WEB-INF/classes -> archive/
                if (Files.exists(tmpLocation.resolve("WEB-INF/classes"))) {
                    Files.move(tmpLocation.resolve("WEB-INF/classes"), appLocation);
                } else {
                    Files.createDirectory(appLocation);
                }
                //META-INF -> archive/META-INF/
                if (Files.exists(tmpLocation.resolve("META-INF"))) {
                    Files.move(tmpLocation.resolve("META-INF"), appLocation.resolve("META-INF"));
                }
                //WEB-INF -> archive/WEB-INF
                if (Files.exists(tmpLocation.resolve("WEB-INF"))) {
                    Files.move(tmpLocation.resolve("WEB-INF"), appLocation.resolve("WEB-INF"));
                }
                // Collect all libraries
                if (Files.exists(tmpLocation.resolve("lib"))) {
                    Files.walk(tmpLocation.resolve("lib"), 1).forEach(libraries::add);
                }
            } else {
                appLocation = tmpLocation;
            }

            List<Consumer<BuildChainBuilder>> customizers = new ArrayList<>();
            try {
                // Test class is a bean
                Class<? extends BuildItem> buildItem = Class
                        .forName("io.quarkus.arc.deployment.AdditionalBeanBuildItem").asSubclass(BuildItem.class);
                customizers.add(new Consumer<BuildChainBuilder>() {
                    @Override
                    public void accept(BuildChainBuilder buildChainBuilder) {
                        buildChainBuilder.addBuildStep(new BuildStep() {
                            @Override
                            public void execute(BuildContext context) {
                                try {
                                    Method factoryMethod = buildItem.getMethod("unremovableOf", Class.class);
                                    context.produce((BuildItem) factoryMethod.invoke(null, testJavaClass));
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }).produces(buildItem)
                                .build();
                    }
                });
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }

            Path testClassesLocation;
            try {
                testClassesLocation = PathTestHelper.getTestClassesLocation(testJavaClass);
            } catch (Exception e) {
                // TCK tests are usually located in a dependency jar
                testClassesLocation = new File("target/test-classes").toPath();
            }

            URLClassLoader appCl;
            try {
                BootstrapClassLoaderFactory clFactory = BootstrapClassLoaderFactory.newInstance()
                        .setAppClasses(appLocation)
                        .setParent(testJavaClass.getClassLoader())
                        .setOffline(PropertyUtils.getBooleanOrNull(BootstrapClassLoaderFactory.PROP_OFFLINE))
                        .setLocalProjectsDiscovery(
                                PropertyUtils.getBoolean(BootstrapClassLoaderFactory.PROP_WS_DISCOVERY, true));
                for (Path library : libraries) {
                    clFactory.addToClassPath(library);
                }
                appCl = clFactory.newDeploymentClassLoader();

            } catch (BootstrapException e) {
                throw new IllegalStateException("Failed to create the boostrap class loader", e);
            }

            appClassloader.set(appCl);

            RuntimeRunner runner = RuntimeRunner.builder()
                    .setLaunchMode(LaunchMode.TEST)
                    .setClassLoader(appCl)
                    .setTarget(appLocation)
                    .setFrameworkClassesPath(testClassesLocation)
                    .addChainCustomizers(customizers)
                    .build();

            runner.run();
            runtimeRunner.set(runner);

            // Instantiate the real test instance
            testInstance = TestInstantiator.instantiateTest(Class
                    .forName(testJavaClass.getName(), true, Thread.currentThread().getContextClassLoader()));

        } catch (Exception e) {
            throw new DeploymentException("Unable to start the runtime runner", e);
        }

        ProtocolMetaData metadata = new ProtocolMetaData();
        URI uri = URI.create(TestHTTPResourceManager.getUri());
        metadata.addContext(new HTTPContext(uri.getHost(), uri.getPort()));
        return metadata;
    }

    @Override
    public void undeploy(Archive<?> archive) throws DeploymentException {
        testInstance = null;
        URLClassLoader cl = appClassloader.get();
        if (cl != null) {
            try {
                cl.close();
            } catch (IOException e) {
                LOGGER.warn("Unable to close the deployment classloader: " + appClassloader.get(), e);
            }
        }
        Path location = deploymentLocation.get();
        if (location != null) {
            try {
                Files.walkFileTree(location, new FileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                LOGGER.warn("Unable to delete the deployment dir: " + location, e);
            }
        }
        RuntimeRunner runner = runtimeRunner.get();
        if (runner != null) {
            try {
                runner.close();
            } catch (IOException e) {
                throw new DeploymentException("Unable to close the runtime runner", e);
            }
        }
    }

    @Override
    public void start() throws LifecycleException {
        // No-op
    }

    @Override
    public void stop() throws LifecycleException {
        // No-op
    }

    @Override
    public ProtocolDescription getDefaultProtocol() {
        return new ProtocolDescription("Quarkus");
    }

    @Override
    public void deploy(Descriptor descriptor) throws DeploymentException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void undeploy(Descriptor descriptor) throws DeploymentException {
        throw new UnsupportedOperationException();
    }

}
