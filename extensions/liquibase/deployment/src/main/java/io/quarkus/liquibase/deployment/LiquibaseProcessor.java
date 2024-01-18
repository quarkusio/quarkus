package io.quarkus.liquibase.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;
import static java.util.function.Predicate.not;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

import io.quarkus.agroal.runtime.DataSources;
import io.quarkus.agroal.spi.JdbcDataSourceBuildItem;
import io.quarkus.agroal.spi.JdbcDataSourceSchemaReadyBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.InitTaskBuildItem;
import io.quarkus.deployment.builditem.InitTaskCompletedBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.liquibase.LiquibaseDataSource;
import io.quarkus.liquibase.LiquibaseFactory;
import io.quarkus.liquibase.runtime.LiquibaseBuildTimeConfig;
import io.quarkus.liquibase.runtime.LiquibaseFactoryProducer;
import io.quarkus.liquibase.runtime.LiquibaseRecorder;
import liquibase.change.Change;
import liquibase.change.DatabaseChangeProperty;
import liquibase.change.core.CreateProcedureChange;
import liquibase.change.core.CreateViewChange;
import liquibase.change.core.LoadDataChange;
import liquibase.change.core.SQLFileChange;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.exception.LiquibaseException;
import liquibase.parser.ChangeLogParser;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.plugin.AbstractPluginFactory;
import liquibase.resource.ClassLoaderResourceAccessor;

class LiquibaseProcessor {

    private static final Logger LOGGER = Logger.getLogger(LiquibaseProcessor.class);

    private static final String LIQUIBASE_BEAN_NAME_PREFIX = "liquibase_";

    private static final DotName DATABASE_CHANGE_PROPERTY = DotName.createSimple(DatabaseChangeProperty.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.LIQUIBASE);
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    IndexDependencyBuildItem indexLiquibase() {
        return new IndexDependencyBuildItem("org.liquibase", "liquibase-core");
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    @Record(STATIC_INIT)
    void nativeImageConfiguration(
            LiquibaseRecorder recorder,
            LiquibaseBuildTimeConfig liquibaseBuildConfig,
            List<JdbcDataSourceBuildItem> jdbcDataSourceBuildItems,
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflective,
            BuildProducer<NativeImageResourceBuildItem> resource,
            BuildProducer<ServiceProviderBuildItem> services,
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitialized,
            BuildProducer<NativeImageResourceBundleBuildItem> resourceBundle) {

        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem(liquibase.diff.compare.CompareControl.class.getName()));
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem(
                liquibase.sqlgenerator.core.LockDatabaseChangeLogGenerator.class.getName()));

        reflective.produce(ReflectiveClassBuildItem
                .builder(liquibase.change.AbstractSQLChange.class, liquibase.database.jvm.JdbcConnection.class).methods()
                .build());

        reflective.produce(ReflectiveClassBuildItem
                .builder(combinedIndex.getIndex().getAllKnownSubclasses(AbstractPluginFactory.class).stream()
                        .map(classInfo -> classInfo.name().toString())
                        .toArray(String[]::new))
                .constructors().build());

        reflective.produce(ReflectiveClassBuildItem
                .builder(liquibase.command.CommandFactory.class.getName())
                .constructors().build());

        reflective.produce(ReflectiveClassBuildItem.builder(
                liquibase.parser.ChangeLogParserConfiguration.class.getName(),
                liquibase.GlobalConfiguration.class.getName(),
                liquibase.executor.ExecutorService.class.getName(),
                liquibase.change.ColumnConfig.class.getName(),
                liquibase.change.AddColumnConfig.class.getName(),
                liquibase.change.core.LoadDataColumnConfig.class.getName(),
                liquibase.sql.visitor.PrependSqlVisitor.class.getName(),
                liquibase.sql.visitor.ReplaceSqlVisitor.class.getName(),
                liquibase.sql.visitor.AppendSqlVisitor.class.getName(),
                liquibase.sql.visitor.RegExpReplaceSqlVisitor.class.getName())
                .constructors().methods().fields().build());

        reflective.produce(ReflectiveClassBuildItem.builder(
                liquibase.change.ConstraintsConfig.class.getName())
                .fields().build());

        // liquibase seems to instantiate these types reflectively...
        reflective.produce(ReflectiveClassBuildItem.builder(ConcurrentHashMap.class, ArrayList.class).build());

        // register classes marked with @DatabaseChangeProperty for reflection
        Set<String> classesMarkedWithDatabaseChangeProperty = new HashSet<>();
        for (AnnotationInstance databaseChangePropertyInstance : combinedIndex.getIndex()
                .getAnnotations(DATABASE_CHANGE_PROPERTY)) {
            // the annotation is only supported on methods but let's be safe
            AnnotationTarget annotationTarget = databaseChangePropertyInstance.target();
            if (annotationTarget.kind() == AnnotationTarget.Kind.METHOD) {
                classesMarkedWithDatabaseChangeProperty.add(annotationTarget.asMethod().declaringClass().name().toString());
            }
        }
        reflective.produce(
                ReflectiveClassBuildItem.builder(classesMarkedWithDatabaseChangeProperty.toArray(new String[0]))
                        .constructors().methods().fields().build());

        Collection<String> dataSourceNames = jdbcDataSourceBuildItems.stream()
                .map(JdbcDataSourceBuildItem::getName)
                .collect(Collectors.toSet());

        resource.produce(
                new NativeImageResourceBuildItem(getChangeLogs(dataSourceNames, liquibaseBuildConfig).toArray(new String[0])));

        Stream.of(liquibase.change.Change.class,
                liquibase.changelog.ChangeLogHistoryService.class,
                liquibase.changeset.ChangeSetService.class,
                liquibase.database.Database.class,
                liquibase.database.DatabaseConnection.class,
                liquibase.datatype.LiquibaseDataType.class,
                liquibase.diff.compare.DatabaseObjectComparator.class,
                liquibase.diff.DiffGenerator.class,
                liquibase.diff.output.changelog.ChangeGenerator.class,
                liquibase.executor.Executor.class,
                liquibase.license.LicenseService.class,
                liquibase.lockservice.LockService.class,
                liquibase.logging.LogService.class,
                liquibase.parser.ChangeLogParser.class,
                liquibase.parser.LiquibaseSqlParser.class,
                liquibase.parser.NamespaceDetails.class,
                liquibase.parser.SnapshotParser.class,
                liquibase.precondition.Precondition.class,
                liquibase.report.ShowSummaryGenerator.class,
                liquibase.serializer.ChangeLogSerializer.class,
                liquibase.serializer.SnapshotSerializer.class,
                liquibase.servicelocator.ServiceLocator.class,
                liquibase.snapshot.SnapshotGenerator.class,
                liquibase.sqlgenerator.SqlGenerator.class,
                liquibase.structure.DatabaseObject.class,
                liquibase.logging.mdc.MdcManager.class)
                .forEach(t -> consumeService(t, (serviceClass, implementations) -> {
                    services.produce(
                            new ServiceProviderBuildItem(serviceClass.getName(), implementations.toArray(new String[0])));
                    reflective.produce(ReflectiveClassBuildItem.builder(implementations.toArray(new String[0]))
                            .constructors().methods().build());
                }));

        // Register Precondition services, and the implementation class for reflection while also registering fields for reflection
        consumeService(liquibase.precondition.Precondition.class, (serviceClass, implementations) -> {
            services.produce(new ServiceProviderBuildItem(serviceClass.getName(), implementations.toArray(new String[0])));
            reflective.produce(ReflectiveClassBuildItem.builder(implementations.toArray(new String[0]))
                    .constructors().methods().fields().build());
        });

        // CommandStep implementations are needed
        consumeService(liquibase.command.CommandStep.class, (serviceClass, implementations) -> {
            var filteredImpls = implementations.stream()
                    .filter(not("liquibase.command.core.StartH2CommandStep"::equals))
                    .toArray(String[]::new);
            services.produce(new ServiceProviderBuildItem(serviceClass.getName(), filteredImpls));
            reflective.produce(ReflectiveClassBuildItem.builder(filteredImpls).constructors().build());
            for (String implementation : filteredImpls) {
                runtimeInitialized.produce(new RuntimeInitializedClassBuildItem(implementation));
            }
        });

        // liquibase XSD
        resource.produce(new NativeImageResourceBuildItem(
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.7.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.8.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.9.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.10.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.11.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.12.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.13.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.14.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.15.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.16.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.17.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.18.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.19.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.21.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.22.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.23.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.24.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.25.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-ext.xsd",
                "liquibase.build.properties"));

        // liquibase resource bundles
        resourceBundle.produce(new NativeImageResourceBundleBuildItem("liquibase/i18n/liquibase-core"));
    }

    private void consumeService(Class<?> serviceClass, BiConsumer<Class<?>, Collection<String>> consumer) {
        try {
            String service = "META-INF/services/" + serviceClass.getName();
            Set<String> implementations = ServiceUtil.classNamesNamedIn(Thread.currentThread().getContextClassLoader(),
                    service);
            consumer.accept(serviceClass, implementations);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void createBeans(LiquibaseRecorder recorder,
            List<JdbcDataSourceBuildItem> jdbcDataSourceBuildItems,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer) {

        // make a LiquibaseContainerProducer bean
        additionalBeans
                .produce(AdditionalBeanBuildItem.builder().addBeanClasses(LiquibaseFactoryProducer.class).setUnremovable()
                        .setDefaultScope(DotNames.SINGLETON).build());
        // add the @LiquibaseDataSource class otherwise it won't be registered as a qualifier
        additionalBeans.produce(AdditionalBeanBuildItem.builder().addBeanClass(LiquibaseDataSource.class).build());

        Collection<String> dataSourceNames = getDataSourceNames(jdbcDataSourceBuildItems);

        for (String dataSourceName : dataSourceNames) {
            SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                    .configure(LiquibaseFactory.class)
                    .scope(ApplicationScoped.class) // this is what the existing code does, but it doesn't seem reasonable
                    .setRuntimeInit()
                    .unremovable()
                    .addInjectionPoint(ClassType.create(DotName.createSimple(LiquibaseFactoryProducer.class)))
                    .addInjectionPoint(ClassType.create(DotName.createSimple(DataSources.class)))
                    .createWith(recorder.liquibaseFunction(dataSourceName));

            if (DataSourceUtil.isDefault(dataSourceName)) {
                configurator.addQualifier(Default.class);
            } else {
                String beanName = LIQUIBASE_BEAN_NAME_PREFIX + dataSourceName;
                configurator.name(beanName);

                configurator.addQualifier().annotation(DotNames.NAMED).addValue("value", beanName).done();
                configurator.addQualifier().annotation(LiquibaseDataSource.class).addValue("value", dataSourceName).done();
            }

            syntheticBeanBuildItemBuildProducer.produce(configurator.done());
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    @Consume(BeanContainerBuildItem.class)
    ServiceStartBuildItem startLiquibase(LiquibaseRecorder recorder,
            List<JdbcDataSourceBuildItem> jdbcDataSourceBuildItems,
            BuildProducer<InitTaskCompletedBuildItem> initializationCompleteBuildItem,
            BuildProducer<JdbcDataSourceSchemaReadyBuildItem> schemaReadyBuildItem) {

        Set<String> dataSourceNames = getDataSourceNames(jdbcDataSourceBuildItems);
        for (String dataSourceName : dataSourceNames) {
            recorder.doStartActions(dataSourceName);
        }
        // once we are done running the migrations, we produce a build item indicating that the
        // schema is "ready"
        schemaReadyBuildItem.produce(new JdbcDataSourceSchemaReadyBuildItem(dataSourceNames));
        initializationCompleteBuildItem.produce(new InitTaskCompletedBuildItem("liquibase"));

        return new ServiceStartBuildItem("liquibase");
    }

    @BuildStep
    public InitTaskBuildItem configureInitTask(ApplicationInfoBuildItem app) {
        return InitTaskBuildItem.create()
                .withName(app.getName() + "-liquibase-init")
                .withTaskEnvVars(Map.of("QUARKUS_INIT_AND_EXIT", "true", "QUARKUS_LIQUIBASE_ENABLED", "true"))
                .withAppEnvVars(Map.of("QUARKUS_LIQUIBASE_ENABLED", "false"))
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

    /**
     * Collect the configured changeLog file for the default and all named datasources.
     * <p>
     * A {@link LinkedHashSet} is used to avoid duplications.
     */
    private List<String> getChangeLogs(Collection<String> dataSourceNames, LiquibaseBuildTimeConfig liquibaseBuildConfig) {
        if (dataSourceNames.isEmpty()) {
            return Collections.emptyList();
        }

        ChangeLogParameters changeLogParameters = new ChangeLogParameters();

        ChangeLogParserFactory changeLogParserFactory = ChangeLogParserFactory.getInstance();

        Set<String> resources = new LinkedHashSet<>();

        ClassLoaderResourceAccessor classLoaderResourceAccessor = new ClassLoaderResourceAccessor(
                Thread.currentThread().getContextClassLoader());

        try {
            // default datasource
            if (DataSourceUtil.hasDefault(dataSourceNames)) {
                resources.addAll(findAllChangeLogFiles(liquibaseBuildConfig.defaultDataSource.changeLog, changeLogParserFactory,
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
                        findAllChangeLogFiles(namedDataSourceChangeLog, changeLogParserFactory, classLoaderResourceAccessor,
                                changeLogParameters));
            }

            LOGGER.debugf("Liquibase changeLogs: %s", resources);

            return new ArrayList<>(resources);

        } finally {
            try {
                classLoaderResourceAccessor.close();
            } catch (Exception ignored) {
                // close() really shouldn't declare that exception, see also https://github.com/liquibase/liquibase/pull/2576
            }
        }
    }

    /**
     * Finds all resource files for the given change log file
     */
    private Set<String> findAllChangeLogFiles(String file, ChangeLogParserFactory changeLogParserFactory,
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

                    changeSet.getChanges().stream()
                            .map(change -> extractChangeFile(change, changeSet.getFilePath()))
                            .forEach(changeFile -> changeFile.ifPresent(result::add));

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

    private Optional<String> extractChangeFile(Change change, String changeSetFilePath) {
        String path = null;
        Boolean relative = null;
        if (change instanceof LoadDataChange) {
            LoadDataChange loadDataChange = (LoadDataChange) change;
            path = loadDataChange.getFile();
            relative = loadDataChange.isRelativeToChangelogFile();
        } else if (change instanceof SQLFileChange) {
            SQLFileChange sqlFileChange = (SQLFileChange) change;
            path = sqlFileChange.getPath();
            relative = sqlFileChange.isRelativeToChangelogFile();
        } else if (change instanceof CreateProcedureChange) {
            CreateProcedureChange createProcedureChange = (CreateProcedureChange) change;
            path = createProcedureChange.getPath();
            relative = createProcedureChange.isRelativeToChangelogFile();
        } else if (change instanceof CreateViewChange) {
            CreateViewChange createViewChange = (CreateViewChange) change;
            path = createViewChange.getPath();
            relative = createViewChange.getRelativeToChangelogFile();
        }

        // unrelated change or change does not reference a file (e.g. inline view)
        if (path == null) {
            return Optional.empty();
        }
        // absolute file path or changeSet has no file path
        if (relative == null || !relative || changeSetFilePath == null) {
            return Optional.of(path);
        }

        // relative file path needs to be resolved against changeSetFilePath
        // notes: ClassLoaderResourceAccessor does not provide a suitable method and CLRA.getFinalPath() is not visible
        return Optional.of(Paths.get(changeSetFilePath).resolveSibling(path).toString().replace('\\', '/'));
    }
}
