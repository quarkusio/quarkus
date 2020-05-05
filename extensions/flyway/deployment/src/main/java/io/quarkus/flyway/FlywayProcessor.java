package io.quarkus.flyway;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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

import io.quarkus.agroal.deployment.JdbcDataSourceBuildItem;
import io.quarkus.agroal.deployment.JdbcDataSourceSchemaReadyBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.flyway.runtime.FlywayBuildTimeConfig;
import io.quarkus.flyway.runtime.FlywayProducer;
import io.quarkus.flyway.runtime.FlywayRecorder;
import io.quarkus.flyway.runtime.FlywayRuntimeConfig;

class FlywayProcessor {

    private static final String CLASSPATH_APPLICATION_MIGRATIONS_PROTOCOL = "classpath";
    private static final String JAR_APPLICATION_MIGRATIONS_PROTOCOL = "jar";
    private static final String FILE_APPLICATION_MIGRATIONS_PROTOCOL = "file";

    private static final Logger LOGGER = Logger.getLogger(FlywayProcessor.class);

    FlywayBuildTimeConfig flywayBuildConfig;

    @BuildStep
    CapabilityBuildItem capability() {
        return new CapabilityBuildItem(Capabilities.FLYWAY);
    }

    @Record(STATIC_INIT)
    @BuildStep
    void build(BuildProducer<AdditionalBeanBuildItem> additionalBeanProducer,
            BuildProducer<FeatureBuildItem> featureProducer,
            BuildProducer<NativeImageResourceBuildItem> resourceProducer,
            FlywayRecorder recorder,
            List<JdbcDataSourceBuildItem> jdbcDataSourceBuildItems,
            BuildProducer<GeneratedBeanBuildItem> generatedBeanBuildItem) throws IOException, URISyntaxException {

        featureProducer.produce(new FeatureBuildItem(FeatureBuildItem.FLYWAY));

        AdditionalBeanBuildItem unremovableProducer = AdditionalBeanBuildItem.unremovableOf(FlywayProducer.class);
        additionalBeanProducer.produce(unremovableProducer);

        Collection<String> dataSourceNames = jdbcDataSourceBuildItems.stream()
                .map(i -> i.getName())
                .collect(Collectors.toSet());
        new FlywayDatasourceBeanGenerator(dataSourceNames, generatedBeanBuildItem).createFlywayProducerBean();

        List<String> applicationMigrations = discoverApplicationMigrations(getMigrationLocations(dataSourceNames));
        recorder.setApplicationMigrationFiles(applicationMigrations);

        resourceProducer.produce(new NativeImageResourceBuildItem(applicationMigrations.toArray(new String[0])));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    ServiceStartBuildItem configureRuntimeProperties(FlywayRecorder recorder,
            FlywayRuntimeConfig flywayRuntimeConfig,
            BeanContainerBuildItem beanContainer,
            List<JdbcDataSourceBuildItem> jdbcDataSourceBuildItems,
            BuildProducer<JdbcDataSourceSchemaReadyBuildItem> schemaReadyBuildItem) {
        recorder.doStartActions(flywayRuntimeConfig, beanContainer.getValue());
        // once we are done running the migrations, we produce a build item indicating that the
        // schema is "ready"
        Collection<String> dataSourceNames = jdbcDataSourceBuildItems.stream()
                .map(i -> i.getName())
                .collect(Collectors.toSet());
        schemaReadyBuildItem.produce(new JdbcDataSourceSchemaReadyBuildItem(dataSourceNames));
        return new ServiceStartBuildItem("flyway");
    }

    /**
     * Collects the configured migration locations for the default and all named DataSources.
     */
    private Collection<String> getMigrationLocations(Collection<String> dataSourceNames) {
        Collection<String> migrationLocations = dataSourceNames.stream()
                .map(flywayBuildConfig::getConfigForDataSourceName)
                .flatMap(config -> config.locations.stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (DataSourceUtil.hasDefault(dataSourceNames)) {
            migrationLocations.addAll(flywayBuildConfig.defaultDataSource.locations);
        }

        return migrationLocations;
    }

    private List<String> discoverApplicationMigrations(Collection<String> locations) throws IOException, URISyntaxException {
        try {
            List<String> applicationMigrationResources = new ArrayList<>();
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
                        applicationMigrationResources.addAll(applicationMigrations);
                    }
                }
            }
            return applicationMigrationResources;
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
