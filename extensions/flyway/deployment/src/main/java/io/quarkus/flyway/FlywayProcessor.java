package io.quarkus.flyway;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.io.File;
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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;

import org.flywaydb.core.Flyway;
import org.jboss.logging.Logger;

import io.quarkus.agroal.deployment.JdbcDataSourceBuildItem;
import io.quarkus.agroal.deployment.JdbcDataSourceSchemaReadyBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
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
import io.quarkus.flyway.runtime.FlywayContainerProducer;
import io.quarkus.flyway.runtime.FlywayRecorder;

class FlywayProcessor {

    private static final String CLASSPATH_APPLICATION_MIGRATIONS_PROTOCOL = "classpath";
    private static final String JAR_APPLICATION_MIGRATIONS_PROTOCOL = "jar";
    private static final String FILE_APPLICATION_MIGRATIONS_PROTOCOL = "file";

    private static final String FLYWAY_BEAN_NAME_PREFIX = "flyway_";

    private static final Logger LOGGER = Logger.getLogger(FlywayProcessor.class);

    FlywayBuildTimeConfig flywayBuildConfig;

    @BuildStep
    CapabilityBuildItem capability() {
        return new CapabilityBuildItem(Capabilities.FLYWAY);
    }

    @Record(STATIC_INIT)
    @BuildStep
    void build(BuildProducer<FeatureBuildItem> featureProducer,
            BuildProducer<NativeImageResourceBuildItem> resourceProducer,
            FlywayRecorder recorder,
            List<JdbcDataSourceBuildItem> jdbcDataSourceBuildItems) throws IOException, URISyntaxException {

        featureProducer.produce(new FeatureBuildItem(FeatureBuildItem.FLYWAY));

        Collection<String> dataSourceNames = getDataSourceNames(jdbcDataSourceBuildItems);

        List<String> applicationMigrations = discoverApplicationMigrations(getMigrationLocations(dataSourceNames));
        recorder.setApplicationMigrationFiles(applicationMigrations);

        resourceProducer.produce(new NativeImageResourceBuildItem(applicationMigrations.toArray(new String[0])));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    ServiceStartBuildItem createBeansAndStartActions(FlywayRecorder recorder,
            List<JdbcDataSourceBuildItem> jdbcDataSourceBuildItems,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer,
            BuildProducer<JdbcDataSourceSchemaReadyBuildItem> schemaReadyBuildItem) {

        // make a FlywayContainerProducer bean
        additionalBeans.produce(AdditionalBeanBuildItem.builder().addBeanClasses(FlywayContainerProducer.class).setUnremovable()
                .setDefaultScope(DotNames.SINGLETON).build());
        // add the @FlywayDataSource class otherwise it won't registered as a qualifier
        additionalBeans.produce(AdditionalBeanBuildItem.builder().addBeanClass(FlywayDataSource.class).build());

        Collection<String> dataSourceNames = getDataSourceNames(jdbcDataSourceBuildItems);

        for (String dataSourceName : dataSourceNames) {
            SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                    .configure(Flyway.class)
                    .scope(Dependent.class) // this is what the existing code does, but it doesn't seem reasonable
                    .setRuntimeInit()
                    .unremovable()
                    .supplier(recorder.flywaySupplier(dataSourceName));

            if (DataSourceUtil.isDefault(dataSourceName)) {
                configurator.addQualifier(Default.class);
            } else {
                String beanName = FLYWAY_BEAN_NAME_PREFIX + dataSourceName;
                configurator.name(beanName);

                configurator.addQualifier().annotation(DotNames.NAMED).addValue("value", beanName).done();
                configurator.addQualifier().annotation(FlywayDataSource.class).addValue("value", dataSourceName).done();
            }

            syntheticBeanBuildItemBuildProducer.produce(configurator.done());
        }

        // will actually run the actions at runtime
        recorder.doStartActions();

        // once we are done running the migrations, we produce a build item indicating that the
        // schema is "ready"
        schemaReadyBuildItem.produce(new JdbcDataSourceSchemaReadyBuildItem(dataSourceNames));

        return new ServiceStartBuildItem("flyway");
    }

    private Set<String> getDataSourceNames(List<JdbcDataSourceBuildItem> jdbcDataSourceBuildItems) {
        Set<String> result = new HashSet<>(jdbcDataSourceBuildItems.size());
        for (JdbcDataSourceBuildItem item : jdbcDataSourceBuildItems) {
            result.add(item.getName());
        }
        return result;
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
                    .map(it -> it.replace(File.separatorChar, '/'))
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
