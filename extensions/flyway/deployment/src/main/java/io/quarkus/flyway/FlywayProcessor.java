package io.quarkus.flyway;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.logging.Logger;

import io.quarkus.agroal.deployment.DataSourceInitializedBuildItem;
import io.quarkus.agroal.deployment.DataSourceSchemaReadyBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanContainerListenerBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.flyway.runtime.FlywayBuildTimeConfig;
import io.quarkus.flyway.runtime.FlywayProducer;
import io.quarkus.flyway.runtime.FlywayRecorder;
import io.quarkus.flyway.runtime.FlywayRuntimeConfig;
import io.quarkus.flyway.runtime.graal.QuarkusPathLocationScanner;

class FlywayProcessor {

    private static final String CLASSPATH_APPLICATION_MIGRATIONS_PROTOCOL = "classpath";
    private static final String JAR_APPLICATION_MIGRATIONS_PROTOCOL = "jar";
    private static final String FILE_APPLICATION_MIGRATIONS_PROTOCOL = "file";

    private static final Logger LOGGER = Logger.getLogger(FlywayProcessor.class);
    /**
     * Flyway build config
     */
    FlywayBuildTimeConfig flywayBuildConfig;

    @BuildStep
    CapabilityBuildItem capability() {
        return new CapabilityBuildItem(Capabilities.FLYWAY);
    }

    @Record(STATIC_INIT)
    @BuildStep(loadsApplicationClasses = true)
    void build(BuildProducer<AdditionalBeanBuildItem> additionalBeanProducer,
            BuildProducer<FeatureBuildItem> featureProducer,
            BuildProducer<NativeImageResourceBuildItem> resourceProducer,
            BuildProducer<BeanContainerListenerBuildItem> containerListenerProducer,
            BuildProducer<GeneratedResourceBuildItem> generatedResourceProducer,
            FlywayRecorder recorder,
            DataSourceInitializedBuildItem dataSourceInitializedBuildItem,
            BuildProducer<GeneratedBeanBuildItem> generatedBeanBuildItem,
            RecorderContext recorderContext) throws IOException, URISyntaxException {

        featureProducer.produce(new FeatureBuildItem(FeatureBuildItem.FLYWAY));

        AdditionalBeanBuildItem unremovableProducer = AdditionalBeanBuildItem.unremovableOf(FlywayProducer.class);
        additionalBeanProducer.produce(unremovableProducer);

        Collection<String> dataSourceNames = DataSourceInitializedBuildItem.dataSourceNamesOf(dataSourceInitializedBuildItem);
        new FlywayDatasourceBeanGenerator(dataSourceNames, generatedBeanBuildItem).createFlywayProducerBean();

        registerNativeImageResources(resourceProducer, generatedResourceProducer,
                discoverApplicationMigrations(getMigrationLocations(dataSourceInitializedBuildItem)));

        containerListenerProducer.produce(
                new BeanContainerListenerBuildItem(recorder.setFlywayBuildConfig(flywayBuildConfig)));
    }

    /**
     * Handles all the operations that can be recorded in the RUNTIME_INIT execution time phase
     *
     * @param recorder Used to set the runtime config
     * @param flywayRuntimeConfig The Flyway configuration
     * @param dataSourceInitializedBuildItem Added this dependency to be sure that Agroal is initialized first
     */
    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    ServiceStartBuildItem configureRuntimeProperties(FlywayRecorder recorder,
            FlywayRuntimeConfig flywayRuntimeConfig,
            BeanContainerBuildItem beanContainer,
            DataSourceInitializedBuildItem dataSourceInitializedBuildItem,
            BuildProducer<DataSourceSchemaReadyBuildItem> schemaReadyBuildItem) {
        recorder.configureFlywayProperties(flywayRuntimeConfig, beanContainer.getValue());
        recorder.doStartActions(flywayRuntimeConfig, beanContainer.getValue());
        // once we are done running the migrations, we produce a build item indicating that the
        // schema is "ready"
        final Collection<String> dataSourceNames = DataSourceInitializedBuildItem
                .dataSourceNamesOf(dataSourceInitializedBuildItem);
        schemaReadyBuildItem.produce(new DataSourceSchemaReadyBuildItem(dataSourceNames));
        return new ServiceStartBuildItem("flyway");
    }

    private void registerNativeImageResources(BuildProducer<NativeImageResourceBuildItem> resource,
            BuildProducer<GeneratedResourceBuildItem> generatedResourceProducer,
            List<String> applicationMigrations)
            throws IOException, URISyntaxException {
        final List<String> nativeResources = new ArrayList<>();
        nativeResources.addAll(applicationMigrations);
        // Store application migration in a generated resource that will be accessed later by the Quarkus-Flyway path scanner
        String resourcesList = applicationMigrations
                .stream()
                .collect(Collectors.joining("\n", "", "\n"));
        generatedResourceProducer.produce(
                new GeneratedResourceBuildItem(
                        QuarkusPathLocationScanner.MIGRATIONS_LIST_FILE,
                        resourcesList.getBytes(StandardCharsets.UTF_8)));
        nativeResources.add(QuarkusPathLocationScanner.MIGRATIONS_LIST_FILE);
        resource.produce(new NativeImageResourceBuildItem(nativeResources.toArray(new String[0])));
    }

    /**
     * Collects the configured migration locations for the default and all named DataSources.
     * <p>
     * A {@link LinkedHashSet} is used to avoid duplications.
     *
     * @param dataSourceInitializedBuildItem {@link DataSourceInitializedBuildItem}
     * @return {@link Collection} of {@link String}s
     */
    private Collection<String> getMigrationLocations(DataSourceInitializedBuildItem dataSourceInitializedBuildItem) {
        Collection<String> dataSourceNames = DataSourceInitializedBuildItem.dataSourceNamesOf(dataSourceInitializedBuildItem);
        Collection<String> migrationLocations = dataSourceNames.stream()
                .map(flywayBuildConfig::getConfigForDataSourceName)
                .flatMap(config -> config.locations.stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (DataSourceInitializedBuildItem.isDefaultDataSourcePresent(dataSourceInitializedBuildItem)) {
            migrationLocations.addAll(flywayBuildConfig.defaultDataSource.locations);
        }
        return migrationLocations;
    }

    private List<String> discoverApplicationMigrations(Collection<String> locations)
            throws IOException, URISyntaxException {
        List<String> resources = new ArrayList<>();
        try {
            // Locations can be a comma separated list
            for (String location : locations) {
                // Strip any 'classpath:' protocol prefixes because they are assumed
                // but not recognized by ClassLoader.getResources()
                if (location != null && location.startsWith(CLASSPATH_APPLICATION_MIGRATIONS_PROTOCOL + ':')) {
                    location = location.substring(CLASSPATH_APPLICATION_MIGRATIONS_PROTOCOL.length() + 1);
                }
                Enumeration<URL> migrations = Thread.currentThread().getContextClassLoader().getResources(location);
                while (migrations.hasMoreElements()) {
                    URL path = migrations.nextElement();
                    LOGGER.infov("Adding application migrations in path ''{0}'' using protocol ''{1}''", path.getPath(),
                            path.getProtocol());
                    final Set<String> applicationMigrations;
                    if (JAR_APPLICATION_MIGRATIONS_PROTOCOL.equals(path.getProtocol())) {
                        try (final FileSystem fileSystem = initFileSystem(path.toURI())) {
                            applicationMigrations = getApplicationMigrationsFromPath(location, path);
                        }
                    } else if (FILE_APPLICATION_MIGRATIONS_PROTOCOL.equals(path.getProtocol())) {
                        applicationMigrations = getApplicationMigrationsFromPath(location, path);
                    } else {
                        LOGGER.warnv(
                                "Unsupported URL protocol ''{0}'' for path ''{1}''. Migration files will not be discovered.",
                                path.getProtocol(), path.getPath());
                        applicationMigrations = null;
                    }
                    if (applicationMigrations != null) {
                        resources.addAll(applicationMigrations);
                    }
                }
            }
            return resources;
        } catch (IOException | URISyntaxException e) {
            throw e;
        }
    }

    private Set<String> getApplicationMigrationsFromPath(final String location, final URL path)
            throws IOException, URISyntaxException {
        try (final Stream<Path> pathStream = Files.walk(Paths.get(path.toURI()))) {
            return pathStream.filter(Files::isRegularFile)
                    .map(it -> Paths.get(location, it.getFileName().toString()).toString())
                    .peek(it -> LOGGER.debug("Discovered: " + it))
                    .collect(Collectors.toSet());
        }
    }

    private FileSystem initFileSystem(final URI uri) throws IOException {
        final Map<String, String> env = new HashMap<>();
        env.put("create", "true");
        return FileSystems.newFileSystem(uri, env);
    }

}
