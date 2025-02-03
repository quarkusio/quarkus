package io.quarkus.flyway.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import jakarta.enterprise.inject.Default;
import jakarta.inject.Singleton;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.migration.JavaMigration;
import org.flywaydb.core.extensibility.ConfigurationExtension;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

import io.quarkus.agroal.deployment.AgroalDataSourceBuildUtil;
import io.quarkus.agroal.spi.JdbcDataSourceBuildItem;
import io.quarkus.agroal.spi.JdbcDataSourceSchemaReadyBuildItem;
import io.quarkus.agroal.spi.JdbcInitialSQLGeneratorBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeansRuntimeInitBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.InitTaskBuildItem;
import io.quarkus.deployment.builditem.InitTaskCompletedBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.flyway.FlywayDataSource;
import io.quarkus.flyway.runtime.FlywayBuildTimeConfig;
import io.quarkus.flyway.runtime.FlywayContainer;
import io.quarkus.flyway.runtime.FlywayContainerProducerImpl;
import io.quarkus.flyway.runtime.FlywayDataSourceBuildTimeConfig;
import io.quarkus.flyway.runtime.FlywayRecorder;
import io.quarkus.flyway.runtime.FlywayRuntimeConfig;
import io.quarkus.runtime.util.ClassPathUtils;

@BuildSteps(onlyIf = FlywayEnabled.class)
class FlywayProcessor {

    private static final String MODEL_CLASS_SUFFIX = "Model";
    private static final String CLASSPATH_APPLICATION_MIGRATIONS_PROTOCOL = "classpath";

    private static final String FLYWAY_CONTAINER_BEAN_NAME_PREFIX = "flyway_container_";
    private static final String FLYWAY_BEAN_NAME_PREFIX = "flyway_";

    private static final DotName JAVA_MIGRATION = DotName.createSimple(JavaMigration.class.getName());

    private static final Logger LOGGER = Logger.getLogger(FlywayProcessor.class);

    @BuildStep
    void reflection(CombinedIndexBuildItem index, BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchyProducer) {
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(ConfigurationExtension.class)
                .reason(getClass().getName())
                .fields().methods().build());

        for (ClassInfo configurationExtension : index.getIndex().getAllKnownImplementors(ConfigurationExtension.class)) {
            var extensionName = configurationExtension.name();
            // we also register Model from the extension fields so that 'ConfigurationExtension#copy' works
            var reflectiveHierarchyItem = ReflectiveHierarchyBuildItem
                    .builder(extensionName)
                    .ignoreTypePredicate(
                            type -> !extensionName.equals(type) && !type.toString().endsWith(MODEL_CLASS_SUFFIX))
                    .ignoreMethodPredicate(m -> !extensionName.equals(m.declaringClass().name()))
                    .ignoreFieldPredicate(f -> !extensionName.equals(f.declaringClass().name())
                            && !f.type().name().toString().endsWith(MODEL_CLASS_SUFFIX))
                    .build();
            reflectiveHierarchyProducer.produce(reflectiveHierarchyItem);
        }
    }

    @BuildStep
    void prepare(BuildProducer<FlywayBuildItem> flywayBuildItems, BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            List<JdbcDataSourceBuildItem> jdbcDataSourceBuildItems,
            FlywayBuildTimeConfig flywayBuildTimeConfig) {

        // make a FlywayContainerProducer bean
        additionalBeans
                .produce(AdditionalBeanBuildItem.builder().addBeanClasses(FlywayContainerProducerImpl.class).setUnremovable()
                        .setDefaultScope(DotNames.SINGLETON).build());
        // add the @FlywayDataSource class otherwise it won't be registered as a qualifier
        additionalBeans.produce(AdditionalBeanBuildItem.builder().addBeanClass(FlywayDataSource.class).build());

        for (JdbcDataSourceBuildItem item : jdbcDataSourceBuildItems) {
            String dataSourceName = item.getName();
            FlywayDataSourceBuildTimeConfig buildTimeConfig = flywayBuildTimeConfig
                    .getConfigForDataSourceName(dataSourceName);
            AnnotationInstance qualifier;
            int priority;
            String flywayBeanName = null;
            String containerBeanName = null;

            if (DataSourceUtil.isDefault(dataSourceName)) {
                // Flyway containers used to be ordered with the default database coming first.
                // Some multitenant tests are relying on this order.
                priority = 10;
                qualifier = AnnotationInstance.builder(Default.class).build();
            } else {
                priority = 5;
                qualifier = AnnotationInstance.builder(FlywayDataSource.class).add("value", dataSourceName)
                        .build();
                flywayBeanName = FLYWAY_BEAN_NAME_PREFIX + dataSourceName;
                containerBeanName = FLYWAY_CONTAINER_BEAN_NAME_PREFIX + dataSourceName;
            }

            flywayBuildItems
                    .produce(new FlywayBuildItem(FlywayContainerProducerImpl.class, buildTimeConfig, "flyway", dataSourceName,
                            dataSourceName, qualifier, priority, containerBeanName, flywayBeanName));
        }
    }

    @Record(STATIC_INIT)
    @BuildStep
    MigrationStateBuildItem build(BuildProducer<NativeImageResourceBuildItem> resourceProducer,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassProducer,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentProducer,
            FlywayRecorder recorder,
            RecorderContext context,
            CombinedIndexBuildItem combinedIndexBuildItem,
            List<FlywayBuildItem> flywayBuildItems,
            FlywayBuildTimeConfig flywayBuildTimeConfig) throws Exception {

        Map<String, Collection<String>> applicationMigrationsToInstance = new HashMap<>();
        for (var flywayBuildItem : flywayBuildItems) {
            FlywayDataSourceBuildTimeConfig flywayDataSourceBuildTimeConfig = flywayBuildItem.getBuildTimeConfig();

            Collection<String> migrationLocations = discoverApplicationMigrations(
                    flywayDataSourceBuildTimeConfig.locations);
            applicationMigrationsToInstance.put(flywayBuildItem.getId(), migrationLocations);
        }
        Set<String> instancesWithMigrations = new HashSet<>();
        Set<String> instancesWithoutMigrations = new HashSet<>();
        for (var e : applicationMigrationsToInstance.entrySet()) {
            if (e.getValue().isEmpty()) {
                instancesWithoutMigrations.add(e.getKey());
            } else {
                instancesWithMigrations.add(e.getKey());
            }
        }

        Collection<String> applicationMigrations = applicationMigrationsToInstance.values().stream().collect(HashSet::new,
                AbstractCollection::addAll, HashSet::addAll);
        for (String applicationMigration : applicationMigrations) {
            Location applicationMigrationLocation = new Location(applicationMigration);
            String applicationMigrationPath = applicationMigrationLocation.getPath();

            if ((applicationMigrationPath != null) &&
            // we don't include .class files in the watched files because that messes up live reload
                    !applicationMigrationPath.endsWith(".class")) {
                hotDeploymentProducer.produce(new HotDeploymentWatchedFileBuildItem(applicationMigrationPath));
            }
        }
        recorder.setApplicationMigrationFiles(applicationMigrations);

        Set<Class<? extends JavaMigration>> javaMigrationClasses = new HashSet<>();
        addJavaMigrations(combinedIndexBuildItem.getIndex().getAllKnownImplementors(JAVA_MIGRATION), context,
                reflectiveClassProducer, javaMigrationClasses);
        recorder.setApplicationMigrationClasses(javaMigrationClasses);

        final Map<String, Collection<Callback>> callbacks = FlywayCallbacksLocator.with(
                flywayBuildItems,
                combinedIndexBuildItem,
                reflectiveClassProducer).getCallbacks();
        recorder.setApplicationCallbackClasses(callbacks);

        resourceProducer.produce(new NativeImageResourceBuildItem(applicationMigrations.toArray(new String[0])));
        return new MigrationStateBuildItem(instancesWithMigrations, instancesWithoutMigrations);
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
            reflectiveClassProducer.produce(
                    ReflectiveClassBuildItem.builder(javaMigration.name().toString()).build());
        }
    }

    @BuildStep
    @Produce(SyntheticBeansRuntimeInitBuildItem.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void createBeans(FlywayRecorder recorder,
            List<FlywayBuildItem> flywayBuildItems,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer,
            MigrationStateBuildItem migrationsBuildItem,
            List<JdbcInitialSQLGeneratorBuildItem> sqlGeneratorBuildItems,
            FlywayBuildTimeConfig flywayBuildTimeConfig) {

        for (FlywayBuildItem flywayBuildItem : flywayBuildItems) {

            boolean hasMigrations = migrationsBuildItem.hasMigrations.contains(flywayBuildItem.getId());
            boolean createPossible = false;
            if (!hasMigrations) {
                createPossible = sqlGeneratorBuildItems.stream()
                        .anyMatch(s -> s.getDatabaseName().equals(flywayBuildItem.getDataSourceName()));
            }

            SyntheticBeanBuildItem.ExtendedBeanConfigurator flywayContainerConfigurator = SyntheticBeanBuildItem
                    .configure(FlywayContainer.class)
                    .scope(Singleton.class)
                    .setRuntimeInit()
                    .unremovable()
                    .addQualifier(flywayBuildItem.getQualifier())
                    .priority(flywayBuildItem.getPriority())
                    .addInjectionPoint(ClassType.create(DotName.createSimple(flywayBuildItem.getProducerClass())))
                    .addInjectionPoint(ClassType.create(DotName.createSimple(DataSource.class)),
                            AgroalDataSourceBuildUtil.qualifier(flywayBuildItem.getDataSourceName()))
                    .startup()
                    .checkActive(recorder.flywayCheckActiveSupplier(flywayBuildItem.getDataSourceName()))
                    .createWith(recorder.flywayContainerFunction(flywayBuildItem.getProducerClass(),
                            flywayBuildItem.getDataSourceName(), flywayBuildItem.getName(), flywayBuildItem.getId(),
                            hasMigrations, createPossible));

            if (flywayBuildItem.getContainerBeanName() != null) {
                flywayContainerConfigurator.name(flywayBuildItem.getContainerBeanName());
                flywayContainerConfigurator.addQualifier().annotation(DotNames.NAMED)
                        .addValue("value", flywayBuildItem.getContainerBeanName()).done();
            }

            syntheticBeanBuildItemBuildProducer.produce(flywayContainerConfigurator.done());

            SyntheticBeanBuildItem.ExtendedBeanConfigurator flywayConfigurator = SyntheticBeanBuildItem
                    .configure(Flyway.class)
                    .scope(Singleton.class)
                    .setRuntimeInit()
                    .unremovable()
                    .addQualifier(flywayBuildItem.getQualifier())
                    .priority(flywayBuildItem.getPriority())
                    .addInjectionPoint(ClassType.create(DotName.createSimple(flywayBuildItem.getProducerClass())))
                    .addInjectionPoint(ClassType.create(DotName.createSimple(FlywayContainer.class)),
                            flywayBuildItem.getQualifier())
                    .startup()
                    .checkActive(recorder.flywayCheckActiveSupplier(flywayBuildItem.getDataSourceName()))
                    .createWith(recorder.flywayFunction(flywayBuildItem.getProducerClass(), flywayBuildItem.getName()));

            if (flywayBuildItem.getFlywayBeanName() != null) {
                flywayConfigurator.name(flywayBuildItem.getFlywayBeanName());
                flywayConfigurator.addQualifier().annotation(DotNames.NAMED)
                        .addValue("value", flywayBuildItem.getFlywayBeanName()).done();
            }

            syntheticBeanBuildItemBuildProducer.produce(flywayConfigurator.done());
        }
    }

    @BuildStep
    @Consume(BeanContainerBuildItem.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    public ServiceStartBuildItem startActions(FlywayRecorder recorder,
            FlywayRuntimeConfig config,
            BuildProducer<JdbcDataSourceSchemaReadyBuildItem> schemaReadyBuildItem,
            BuildProducer<InitTaskCompletedBuildItem> initializationCompleteBuildItem,
            List<FlywayBuildItem> flywayBuildItems,
            MigrationStateBuildItem migrationsBuildItem) {

        for (FlywayBuildItem flywayBuildItem : flywayBuildItems) {
            recorder.doStartActions(flywayBuildItem.getProducerClass(), flywayBuildItem.getDataSourceName(), flywayBuildItem.getName());
        }

        // once we are done running the migrations, we produce a build item indicating that the
        // schema is "ready"
        schemaReadyBuildItem.produce(new JdbcDataSourceSchemaReadyBuildItem(migrationsBuildItem.hasMigrations));
        initializationCompleteBuildItem.produce(new InitTaskCompletedBuildItem("flyway"));
        return new ServiceStartBuildItem("flyway");
    }

    @BuildStep
    public InitTaskBuildItem configureInitTask(ApplicationInfoBuildItem app) {
        return InitTaskBuildItem.create()
                .withName(app.getName() + "-flyway-init")
                .withTaskEnvVars(Map.of("QUARKUS_INIT_AND_EXIT", "true", "QUARKUS_FLYWAY_ACTIVE", "true"))
                .withAppEnvVars(Map.of("QUARKUS_FLYWAY_ACTIVE", "false"))
                .withSharedEnvironment(true)
                .withSharedFilesystem(true);
    }

    private Set<String> getDataSourceNames(List<JdbcDataSourceBuildItem> jdbcDataSourceBuildItems) {
        Set<String> result = new HashSet<>(jdbcDataSourceBuildItems.size());
        for (JdbcDataSourceBuildItem item : jdbcDataSourceBuildItems) {
            result.add(item.getName());
        }
        return result;
    }

    private Collection<String> discoverApplicationMigrations(Collection<String> locations)
            throws IOException {
        LinkedHashSet<String> applicationMigrationResources = new LinkedHashSet<>();
        // Locations can be a comma separated list
        for (String location : locations) {
            location = normalizeLocation(location);
            if (location.startsWith(Location.FILESYSTEM_PREFIX)) {
                applicationMigrationResources.add(location);
                continue;
            }

            String finalLocation = location;
            ClassPathUtils.consumeAsPaths(Thread.currentThread().getContextClassLoader(), location, path -> {
                Set<String> applicationMigrations = null;
                try {
                    applicationMigrations = FlywayProcessor.this.getApplicationMigrationsFromPath(finalLocation, path);
                } catch (IOException e) {
                    LOGGER.warnv(e,
                            "Can't process files in path %s", path);
                }
                if (applicationMigrations != null) {
                    applicationMigrationResources.addAll(applicationMigrations);
                }
            });
        }
        return applicationMigrationResources;
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

    private Set<String> getApplicationMigrationsFromPath(final String location, final Path rootPath)
            throws IOException {

        try (final Stream<Path> pathStream = Files.walk(rootPath)) {
            return pathStream.filter(Files::isRegularFile)
                    .map(it -> Paths.get(location, rootPath.relativize(it).toString()).normalize().toString())
                    // we don't want windows paths here since the paths are going to be used as classpath paths anyway
                    .map(it -> it.replace('\\', '/'))
                    .peek(it -> LOGGER.debugf("Discovered path: %s", it))
                    .collect(Collectors.toSet());
        }
    }

    public static final class MigrationStateBuildItem extends SimpleBuildItem {

        final Set<String> hasMigrations;
        final Set<String> missingMigrations;

        MigrationStateBuildItem(Set<String> hasMigrations, Set<String> missingMigrations) {
            this.hasMigrations = hasMigrations;
            this.missingMigrations = missingMigrations;
        }
    }
}
