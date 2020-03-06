package io.quarkus.liquibase;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;
import static io.quarkus.liquibase.runtime.graal.LiquibaseServiceLoader.serviceResourceFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.logging.Logger;

import io.quarkus.agroal.deployment.JdbcDataSourceBuildItem;
import io.quarkus.agroal.deployment.JdbcDataSourceSchemaReadyBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanContainerListenerBuildItem;
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

    /**
     * The change log parser factory singleton used for the liquibase service classpath scanner
     */
    private static final ChangeLogParserFactory CHANGE_LOG_PARSER_FACTORY = ChangeLogParserFactory.getInstance();

    /**
     * Liquibase build time configuration
     */
    LiquibaseBuildTimeConfig liquibaseBuildConfig;

    @BuildStep
    CapabilityBuildItem capability() {
        return new CapabilityBuildItem(Capabilities.LIQUIBASE);
    }

    @BuildStep
    void reflection(BuildProducer<ReflectiveClassBuildItem> reflective,
            BuildProducer<NativeImageResourceBuildItem> resourceProducer,
            BuildProducer<GeneratedResourceBuildItem> generatedResourceProducer,
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitialized) {

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
        Stream.of(liquibase.license.LicenseService.class,
                liquibase.diff.compare.DatabaseObjectComparator.class,
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
                .forEach(t -> addService(reflective, generatedResourceProducer, resourceProducer, t));
    }

    @Record(STATIC_INIT)
    @BuildStep(loadsApplicationClasses = true)
    void build(BuildProducer<AdditionalBeanBuildItem> additionalBeanProducer,
            BuildProducer<FeatureBuildItem> featureProducer,
            BuildProducer<NativeImageResourceBuildItem> resourceProducer,
            BuildProducer<BeanContainerListenerBuildItem> containerListenerProducer,
            LiquibaseRecorder recorder,
            List<JdbcDataSourceBuildItem> jdbcDataSourceBuildItems,
            BuildProducer<GeneratedBeanBuildItem> generatedBeanBuildItem,
            BuildProducer<NativeImageResourceBundleBuildItem> resourceBundle) {

        featureProducer.produce(new FeatureBuildItem(FeatureBuildItem.LIQUIBASE));

        AdditionalBeanBuildItem unremovableProducer = AdditionalBeanBuildItem.unremovableOf(LiquibaseProducer.class);
        additionalBeanProducer.produce(unremovableProducer);

        Collection<String> dataSourceNames = jdbcDataSourceBuildItems.stream()
                .map(i -> i.getName())
                .collect(Collectors.toSet());
        new LiquibaseDatasourceBeanGenerator(dataSourceNames, generatedBeanBuildItem).createLiquibaseProducerBean();

        containerListenerProducer.produce(
                new BeanContainerListenerBuildItem(recorder.setLiquibaseBuildConfig(liquibaseBuildConfig)));

        resourceProducer.produce(
                new NativeImageResourceBuildItem(getChangeLogs(dataSourceNames).toArray(new String[0])));

        // liquibase XSD
        resourceProducer.produce(new NativeImageResourceBuildItem(
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

    /**
     * Handles all the operations that can be recorded in the RUNTIME_INIT execution time phase
     *
     * @param recorder Used to set the runtime config
     * @param liquibaseRuntimeConfig The Liquibase configuration
     */
    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    ServiceStartBuildItem configureRuntimeProperties(LiquibaseRecorder recorder,
            LiquibaseRuntimeConfig liquibaseRuntimeConfig,
            BeanContainerBuildItem beanContainer,
            List<JdbcDataSourceBuildItem> jdbcDataSourceBuildItems,
            BuildProducer<JdbcDataSourceSchemaReadyBuildItem> schemaReadyBuildItem) {
        recorder.configureLiquibaseProperties(liquibaseRuntimeConfig, beanContainer.getValue());
        recorder.doStartActions(liquibaseRuntimeConfig, beanContainer.getValue());
        // once we are done running the migrations, we produce a build item indicating that the
        // schema is "ready"
        Collection<String> dataSourceNames = jdbcDataSourceBuildItems.stream()
                .map(i -> i.getName())
                .collect(Collectors.toSet());
        schemaReadyBuildItem.produce(new JdbcDataSourceSchemaReadyBuildItem(dataSourceNames));
        return new ServiceStartBuildItem("liquibase");
    }

    /**
     * Search for all implementation of the interface {@code className}.
     * The service interface is add to the reflection.
     * Each implementation is add to the reflection and add to the
     * native resource (text file) which contains all the implementation classes.
     * The native resource name {@link LiquibaseServiceLoader#serviceResourceFile(Class)}
     * and to the generated text file list which is add to the native image.
     *
     * @param reflective the reflective producer
     * @param generatedResourceProducer the generated resource producer
     * @param resourceProducer the resource producer
     * @param className the service class
     */
    private void addService(BuildProducer<ReflectiveClassBuildItem> reflective,
            BuildProducer<GeneratedResourceBuildItem> generatedResourceProducer,
            BuildProducer<NativeImageResourceBuildItem> resourceProducer,
            Class<?> className) {

        Class<?>[] impl = ServiceLocator.getInstance().findClasses(className);
        if (impl != null && impl.length > 0) {
            reflective.produce(new ReflectiveClassBuildItem(true, false, false, impl));
            String resourcesList = Arrays.stream(impl)
                    .map(Class::getName)
                    .collect(Collectors.joining("\n", "", "\n"));
            String resourceName = serviceResourceFile(className);
            generatedResourceProducer.produce(
                    new GeneratedResourceBuildItem(resourceName, resourcesList.getBytes(StandardCharsets.UTF_8)));
            resourceProducer.produce(new NativeImageResourceBuildItem(resourceName));
        }
        reflective.produce(new ReflectiveClassBuildItem(false, false, false, className.getName()));
    }

    /**
     * Add the reflection for the liquibase class interface and all the implementation of the interface
     *
     * @param reflective the reflective build producer
     * @param methods the method flag
     * @param className the interface class
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
     * Collects the configured changeLog file for the default and all named DataSources.
     * <p>
     * A {@link LinkedHashSet} is used to avoid duplications.
     *
     * @param dataSourceInitializedBuildItem {@link DataSourceInitializedBuildItem}
     * @return {@link Collection} of {@link String}s
     */
    private List<String> getChangeLogs(Collection<String> dataSourceNames) {
        Collection<String> changeLogs = dataSourceNames.stream()
                .filter(n -> !DataSourceUtil.isDefault(n))
                .map(liquibaseBuildConfig::getConfigForDataSourceName)
                .map(c -> c.changeLog)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        ChangeLogParameters changeLogParameters = new ChangeLogParameters();
        ClassLoaderResourceAccessor classLoaderResourceAccessor = new ClassLoaderResourceAccessor(
                Thread.currentThread().getContextClassLoader());

        Set<String> resources = new LinkedHashSet<>();
        for (String tmp : changeLogs) {
            resources.addAll(findAllChangeLogs(tmp, classLoaderResourceAccessor, changeLogParameters));
        }
        if (DataSourceUtil.hasDefault(dataSourceNames)) {
            resources.addAll(findAllChangeLogs(liquibaseBuildConfig.defaultDataSource.changeLog, classLoaderResourceAccessor,
                    changeLogParameters));
        }
        LOGGER.debugf("Liquibase changeLogs: " + resources);
        return new ArrayList<>(resources);
    }

    /**
     * Finds all changes for the change log file
     *
     * @param file the change log file
     * @param classLoaderResourceAccessor the liquibase class loader resource accessor
     * @param changeLogParameters the liquibase change log parameters
     * @return the corresponding list of change log resources.
     */
    private Set<String> findAllChangeLogs(String file, ClassLoaderResourceAccessor classLoaderResourceAccessor,
            ChangeLogParameters changeLogParameters) {
        try {
            ChangeLogParser parser = CHANGE_LOG_PARSER_FACTORY.getParser(file, classLoaderResourceAccessor);
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
