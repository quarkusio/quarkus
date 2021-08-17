package io.quarkus.flyway;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.migration.JavaMigration;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

import io.quarkus.agroal.spi.JdbcDataSourceBuildItem;
import io.quarkus.agroal.spi.JdbcDataSourceSchemaReadyBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.flyway.runtime.FlywayBuildTimeConfig;
import io.quarkus.flyway.runtime.FlywayContainerProducer;
import io.quarkus.flyway.runtime.FlywayRecorder;

class FlywayProcessor {

    private static final String CLASSPATH_APPLICATION_MIGRATIONS_PROTOCOL = "classpath";
    private static final String JAR_APPLICATION_MIGRATIONS_PROTOCOL = "jar";
    private static final String FILE_APPLICATION_MIGRATIONS_PROTOCOL = "file";

    private static final String FLYWAY_BEAN_NAME_PREFIX = "flyway_";

    private static final DotName JAVA_MIGRATION = DotName.createSimple(JavaMigration.class.getName());

    private static final Logger LOGGER = Logger.getLogger(FlywayProcessor.class);

    FlywayBuildTimeConfig flywayBuildConfig;

    @BuildStep
    IndexDependencyBuildItem indexFlyway() {
        return new IndexDependencyBuildItem("org.flywaydb", "flyway-core");
    }

    @Record(STATIC_INIT)
    @BuildStep
    void build(BuildProducer<FeatureBuildItem> featureProducer,
            BuildProducer<NativeImageResourceBuildItem> resourceProducer,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassProducer,
            FlywayRecorder recorder,
            RecorderContext context,
            CombinedIndexBuildItem combinedIndexBuildItem,
            List<JdbcDataSourceBuildItem> jdbcDataSourceBuildItems) throws Exception {

        featureProducer.produce(new FeatureBuildItem(Feature.FLYWAY));

        Collection<String> dataSourceNames = getDataSourceNames(jdbcDataSourceBuildItems);

        Collection<String> applicationMigrations = discoverApplicationMigrations(getMigrationLocations(dataSourceNames));
        recorder.setApplicationMigrationFiles(applicationMigrations);

        Set<Class<? extends JavaMigration>> javaMigrationClasses = new HashSet<>();
        addJavaMigrations(combinedIndexBuildItem.getIndex().getAllKnownImplementors(JAVA_MIGRATION), context,
                reflectiveClassProducer, javaMigrationClasses);
        recorder.setApplicationMigrationClasses(javaMigrationClasses);

        final Map<String, Collection<Callback>> callbacks = FlywayCallbacksLocator.with(
                dataSourceNames,
                flywayBuildConfig,
                combinedIndexBuildItem,
                reflectiveClassProducer).getCallbacks();
        recorder.setApplicationCallbackClasses(callbacks);

        resourceProducer.produce(new NativeImageResourceBuildItem(applicationMigrations.toArray(new String[0])));
    }

    @SuppressWarnings("unchecked")
    private void addJavaMigrations(Collection<ClassInfo> candidates, RecorderContext context,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassProducer,
            Set<Class<? extends JavaMigration>> javaMigrationClasses) {
        for (ClassInfo javaMigration : candidates) {
            if (Modifier.isAbstract(javaMigration.flags())) {
                continue;
            }
            javaMigrationClasses.add((Class<JavaMigration>) context.classProxy(javaMigration.name().toString()));
            reflectiveClassProducer.produce(new ReflectiveClassBuildItem(false, false, javaMigration.name().toString()));
        }
    }

    @BuildStep
    @Consume(LoggingSetupBuildItem.class)
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

        recorder.resetFlywayContainers();

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
        return migrationLocations;
    }

    private Collection<String> discoverApplicationMigrations(Collection<String> locations)
            throws IOException, URISyntaxException {
        try {
            LinkedHashSet<String> applicationMigrationResources = new LinkedHashSet<>();
            // Locations can be a comma separated list
            for (String location : locations) {
                location = normalizeLocation(location);
                if (location.startsWith(Location.FILESYSTEM_PREFIX)) {
                    applicationMigrationResources.add(location);
                    continue;
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

    private String normalizeLocation(String location) {
        if (location == null) {
            throw new IllegalStateException("Flyway migration location may not be null.");
        }

        // Strip any 'classpath:' protocol prefixes because they are assumed
        // but not recognized by ClassLoader.getResources()
        if (location.startsWith(CLASSPATH_APPLICATION_MIGRATIONS_PROTOCOL + ':')) {
            location = location.substring(CLASSPATH_APPLICATION_MIGRATIONS_PROTOCOL.length() + 1);
            if (location.startsWith("/")) {
                location = location.substring(1);
            }
        }
        if (!location.endsWith("/")) {
            location += "/";
        }

        return location;
    }

    private Set<String> getApplicationMigrationsFromPath(final String location, final URL path)
            throws IOException, URISyntaxException {
        Path rootPath = Paths.get(path.toURI());

        try (final Stream<Path> pathStream = Files.walk(rootPath)) {
            return pathStream.filter(Files::isRegularFile)
                    .map(it -> Paths.get(location, rootPath.relativize(it).toString()).normalize().toString())
                    // we don't want windows paths here since the paths are going to be used as classpath paths anyway
                    .map(it -> it.replace('\\', '/'))
                    .peek(it -> LOGGER.debugf("Discovered path: %s", it))
                    .collect(Collectors.toSet());
        }
    }

    private FileSystem initFileSystem(final URI uri) throws IOException {
        final Map<String, String> env = new HashMap<>();
        env.put("create", "true");
        return FileSystems.newFileSystem(uri, env);
    }

    /**
     * Reinitialize {@code InsertRowLock} to avoid using a cached seed when invoking {@code getNextRandomString}
     */
    @BuildStep
    public RuntimeReinitializedClassBuildItem reinitInsertRowLock() {
        return new RuntimeReinitializedClassBuildItem(
                "org.flywaydb.core.internal.database.InsertRowLock");
    }
}
