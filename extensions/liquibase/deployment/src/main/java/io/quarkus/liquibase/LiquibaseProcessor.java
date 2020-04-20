package io.quarkus.liquibase;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.liquibase.runtime.LiquibaseBuildTimeConfig;
import io.quarkus.liquibase.runtime.LiquibaseProducer;
import io.quarkus.liquibase.runtime.LiquibaseRecorder;
import io.quarkus.liquibase.runtime.LiquibaseRuntimeConfig;
import io.quarkus.liquibase.runtime.graal.LiquibaseServiceLoader;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.exception.LiquibaseException;
import liquibase.parser.ChangeLogParser;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.servicelocator.ServiceLocator;

class LiquibaseProcessor {

    private static final Logger LOGGER = Logger.getLogger(LiquibaseServiceLoader.class);

    LiquibaseBuildTimeConfig liquibaseBuildConfig;

    @BuildStep
    CapabilityBuildItem capability() {
        return new CapabilityBuildItem(Capabilities.LIQUIBASE);
    }

    @BuildStep(onlyIf = NativeBuild.class)
    @Record(STATIC_INIT)
    void nativeImageConfiguration(
            LiquibaseRecorder recorder,
            List<JdbcDataSourceBuildItem> jdbcDataSourceBuildItems,
            BuildProducer<ReflectiveClassBuildItem> reflective,
            BuildProducer<NativeImageResourceBuildItem> resource,
            BuildProducer<GeneratedResourceBuildItem> generatedResource,
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitialized,
            BuildProducer<NativeImageResourceBundleBuildItem> resourceBundle) {

        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("liquibase.util.StringUtils"));
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("liquibase.servicelocator.ServiceLocator"));
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("liquibase.diff.compare.CompareControl"));

        reflective.produce(new ReflectiveClassBuildItem(false, true, false,
                "liquibase.change.AbstractSQLChange",
                "liquibase.database.jvm.JdbcConnection"));

        reflective.produce(new ReflectiveClassBuildItem(true, true, true,
                "liquibase.change.ColumnConfig",
                "liquibase.change.AddColumnConfig"));

        reflective.produce(new ReflectiveClassBuildItem(false, false, true,
                "liquibase.change.ConstraintsConfig"));

        addReflection(reflective, true, liquibase.change.Change.class);

        // add all implementation of these classes to the reflection process
        addReflection(reflective, false,
                liquibase.configuration.ConfigurationContainer.class,
                liquibase.parser.LiquibaseParser.class,
                liquibase.structure.DatabaseObject.class,
                liquibase.sql.visitor.SqlVisitor.class);

        // load the liquibase services
        Map<String, List<String>> serviceClassesImplementationRegistry = new HashMap<>();

        Stream.of(liquibase.diff.compare.DatabaseObjectComparator.class,
                liquibase.parser.NamespaceDetails.class,
                liquibase.precondition.Precondition.class,
                liquibase.database.Database.class,
                liquibase.parser.ChangeLogParser.class,
                liquibase.change.Change.class,
                liquibase.snapshot.SnapshotGenerator.class,
                liquibase.changelog.ChangeLogHistoryService.class,
                liquibase.datatype.LiquibaseDataType.class,
                liquibase.executor.Executor.class,
                liquibase.lockservice.LockService.class,
                liquibase.sqlgenerator.SqlGenerator.class)
                .forEach(t -> addService(reflective, t, true, serviceClassesImplementationRegistry));

        addService(reflective, liquibase.license.LicenseService.class, false, serviceClassesImplementationRegistry);

        recorder.setServicesImplementations(serviceClassesImplementationRegistry);

        Collection<String> dataSourceNames = jdbcDataSourceBuildItems.stream()
                .map(i -> i.getName())
                .collect(Collectors.toSet());

        resource.produce(
                new NativeImageResourceBuildItem(getChangeLogs(dataSourceNames).toArray(new String[0])));

        // liquibase XSD
        resource.produce(new NativeImageResourceBuildItem(
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.5.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.6.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.7.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.8.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.9.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-ext.xsd",
                "www.liquibase.org/xml/ns/pro/liquibase-pro-3.8.xsd",
                "liquibase.build.properties"));

        // liquibase resource bundles
        resourceBundle.produce(new NativeImageResourceBundleBuildItem("liquibase/i18n/liquibase-core"));
    }

    @BuildStep
    void build(
            List<JdbcDataSourceBuildItem> jdbcDataSourceBuildItems,
            BuildProducer<AdditionalBeanBuildItem> additionalBean,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<GeneratedBeanBuildItem> generatedBean) {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.LIQUIBASE));

        AdditionalBeanBuildItem unremovableProducer = AdditionalBeanBuildItem.unremovableOf(LiquibaseProducer.class);
        additionalBean.produce(unremovableProducer);

        Collection<String> dataSourceNames = jdbcDataSourceBuildItems.stream()
                .map(i -> i.getName())
                .collect(Collectors.toSet());
        new LiquibaseDatasourceBeanGenerator(dataSourceNames, generatedBean).createLiquibaseProducerBean();
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    ServiceStartBuildItem configureRuntimeProperties(LiquibaseRecorder recorder,
            LiquibaseRuntimeConfig liquibaseRuntimeConfig,
            BeanContainerBuildItem beanContainer,
            List<JdbcDataSourceBuildItem> jdbcDataSourceBuildItems,
            BuildProducer<JdbcDataSourceSchemaReadyBuildItem> jdbcDataSourceSchemaReady) {
        recorder.doStartActions(liquibaseRuntimeConfig, beanContainer.getValue());
        // once we are done running the migrations, we produce a build item indicating that the
        // schema is "ready"
        Collection<String> dataSourceNames = jdbcDataSourceBuildItems.stream()
                .map(i -> i.getName())
                .collect(Collectors.toSet());
        jdbcDataSourceSchemaReady.produce(new JdbcDataSourceSchemaReadyBuildItem(dataSourceNames));
        return new ServiceStartBuildItem("liquibase");
    }

    /**
     * Search for all implementation of the interface {@code className}.
     * <p>
     * Each implementation is added to the reflection configuration and recorded
     * in a map which used to load the implementations of the service in native image.
     */
    private void addService(BuildProducer<ReflectiveClassBuildItem> reflective, Class<?> className, boolean methods,
            Map<String, List<String>> serviceClassesImplementationRegistry) {

        Class<?>[] classImplementations = ServiceLocator.getInstance().findClasses(className);

        if (classImplementations != null && classImplementations.length > 0) {
            reflective.produce(new ReflectiveClassBuildItem(true, methods, false, classImplementations));
            List<String> serviceImplementations = new ArrayList<>();

            for (Class<?> classImpl : classImplementations) {
                serviceImplementations.add(classImpl.getName());
            }

            serviceClassesImplementationRegistry.put(className.getName(), serviceImplementations);
        }

        reflective.produce(new ReflectiveClassBuildItem(false, false, false, className.getName()));
        reflective.produce(new ReflectiveClassBuildItem(false, false, false, className.getName()));
    }

    /**
     * Add the reflection for the liquibase class interface and all the implementations of the interface
     */
    private void addReflection(BuildProducer<ReflectiveClassBuildItem> reflective, boolean methods, Class<?>... className) {
        for (Class<?> clazz : className) {
            Class<?>[] impl = ServiceLocator.getInstance().findClasses(clazz);
            if (impl != null && impl.length > 0) {
                reflective.produce(new ReflectiveClassBuildItem(true, methods, false, impl));
            }
        }
    }

    /**
     * Collect the configured changeLog file for the default and all named datasources.
     * <p>
     * A {@link LinkedHashSet} is used to avoid duplications.
     */
    private List<String> getChangeLogs(Collection<String> dataSourceNames) {
        if (dataSourceNames.isEmpty()) {
            return Collections.emptyList();
        }

        ChangeLogParameters changeLogParameters = new ChangeLogParameters();
        ClassLoaderResourceAccessor classLoaderResourceAccessor = new ClassLoaderResourceAccessor(
                Thread.currentThread().getContextClassLoader());

        ChangeLogParserFactory changeLogParserFactory = ChangeLogParserFactory.getInstance();

        Set<String> resources = new LinkedHashSet<>();

        // default datasource
        if (DataSourceUtil.hasDefault(dataSourceNames)) {
            resources.addAll(findAllChangeLogs(liquibaseBuildConfig.defaultDataSource.changeLog, changeLogParserFactory,
                    classLoaderResourceAccessor, changeLogParameters));
        }

        // named datasources
        Collection<String> namedDataSourceChangeLogs = dataSourceNames.stream()
                .filter(n -> !DataSourceUtil.isDefault(n))
                .map(liquibaseBuildConfig::getConfigForDataSourceName)
                .map(c -> c.changeLog)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        for (String namedDataSourceChangeLog : namedDataSourceChangeLogs) {
            resources.addAll(
                    findAllChangeLogs(namedDataSourceChangeLog, changeLogParserFactory, classLoaderResourceAccessor,
                            changeLogParameters));
        }

        LOGGER.debugf("Liquibase changeLogs: %s", resources);

        return new ArrayList<>(resources);
    }

    /**
     * Finds all resource files for the given change log file
     */
    private Set<String> findAllChangeLogs(String file, ChangeLogParserFactory changeLogParserFactory,
            ClassLoaderResourceAccessor classLoaderResourceAccessor,
            ChangeLogParameters changeLogParameters) {
        try {
            ChangeLogParser parser = changeLogParserFactory.getParser(file, classLoaderResourceAccessor);
            DatabaseChangeLog changelog = parser.parse(file, changeLogParameters, classLoaderResourceAccessor);

            if (changelog != null) {
                Set<String> result = new LinkedHashSet<>();
                // get all changeSet files
                for (ChangeSet changeSet : changelog.getChangeSets()) {
                    result.add(changeSet.getFilePath());

                    // get all parents of the changeSet
                    DatabaseChangeLog parent = changeSet.getChangeLog();
                    while (parent != null) {
                        result.add(parent.getFilePath());
                        parent = parent.getParentChangeLog();
                    }
                }
                result.add(changelog.getFilePath());
                return result;
            }
        } catch (LiquibaseException ex) {
            throw new IllegalStateException(ex);
        }
        return Collections.emptySet();
    }

}
