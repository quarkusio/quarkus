package io.quarkus.liquibase.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

import io.quarkus.agroal.spi.JdbcDataSourceBuildItem;
import io.quarkus.agroal.spi.JdbcDataSourceSchemaReadyBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeansRuntimeInitBuildItem;
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
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
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
import liquibase.resource.ClassLoaderResourceAccessor;

class LiquibaseProcessor {

    private static final Logger LOGGER = Logger.getLogger(LiquibaseProcessor.class);

    private static final String LIQUIBASE_BEAN_NAME_PREFIX = "liquibase_";

    private static final DotName DATABASE_CHANGE_PROPERTY = DotName.createSimple(DatabaseChangeProperty.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.LIQUIBASE);
    }

    @BuildStep
    public SystemPropertyBuildItem disableHub() {
        // Don't block app startup with prompt:
        // Do you want to see this operation's report in Liquibase Hub, which improves team collaboration?
        // If so, enter your email. If not, enter [N] to no longer be prompted, or [S] to skip for now, but ask again next time (default "S"):
        return new SystemPropertyBuildItem("liquibase.hub.mode", "off");
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

        reflective.produce(new ReflectiveClassBuildItem(false, true, false,
                liquibase.change.AbstractSQLChange.class.getName(),
                liquibase.database.jvm.JdbcConnection.class.getName()));

        reflective.produce(new ReflectiveClassBuildItem(true, false, false, "liquibase.command.LiquibaseCommandFactory"));

        reflective.produce(new ReflectiveClassBuildItem(true, true, true,
                liquibase.parser.ChangeLogParserCofiguration.class.getName(),
                liquibase.hub.HubServiceFactory.class.getName(),
                liquibase.logging.core.DefaultLoggerConfiguration.class.getName(),
                liquibase.configuration.GlobalConfiguration.class.getName(),
                com.datical.liquibase.ext.config.LiquibaseProConfiguration.class.getName(),
                liquibase.license.LicenseServiceFactory.class.getName(),
                liquibase.executor.ExecutorService.class.getName(),
                liquibase.change.ChangeFactory.class.getName(),
                liquibase.logging.core.LogServiceFactory.class.getName(),
                liquibase.logging.LogFactory.class.getName(),
                liquibase.change.ColumnConfig.class.getName(),
                liquibase.change.AddColumnConfig.class.getName(),
                liquibase.change.core.LoadDataColumnConfig.class.getName(),
                liquibase.sql.visitor.PrependSqlVisitor.class.getName(),
                liquibase.sql.visitor.ReplaceSqlVisitor.class.getName(),
                liquibase.sql.visitor.AppendSqlVisitor.class.getName(),
                liquibase.sql.visitor.RegExpReplaceSqlVisitor.class.getName()));

        reflective.produce(new ReflectiveClassBuildItem(false, false, true,
                liquibase.change.ConstraintsConfig.class.getName()));

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
                new ReflectiveClassBuildItem(true, true, true, classesMarkedWithDatabaseChangeProperty.toArray(new String[0])));

        Collection<String> dataSourceNames = jdbcDataSourceBuildItems.stream()
                .map(i -> i.getName())
                .collect(Collectors.toSet());

        resource.produce(
                new NativeImageResourceBuildItem(getChangeLogs(dataSourceNames, liquibaseBuildConfig).toArray(new String[0])));

        Stream.of(liquibase.change.Change.class,
                liquibase.changelog.ChangeLogHistoryService.class,
                liquibase.command.LiquibaseCommand.class,
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
                liquibase.parser.NamespaceDetails.class,
                liquibase.parser.SnapshotParser.class,
                liquibase.precondition.Precondition.class,
                liquibase.serializer.ChangeLogSerializer.class,
                liquibase.serializer.SnapshotSerializer.class,
                liquibase.servicelocator.ServiceLocator.class,
                liquibase.snapshot.SnapshotGenerator.class,
                liquibase.sqlgenerator.SqlGenerator.class,
                liquibase.structure.DatabaseObject.class,
                liquibase.hub.HubService.class)
                .forEach(t -> addService(services, reflective, t, false));

        // Register Precondition services, and the implementation class for reflection while also registering fields for reflection
        addService(services, reflective, liquibase.precondition.Precondition.class, true);

        // liquibase XSD
        resource.produce(new NativeImageResourceBuildItem(
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.5.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.6.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.7.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.8.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.9.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.10.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.0.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.1.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-ext.xsd",
                "www.liquibase.org/xml/ns/pro/liquibase-pro-3.8.xsd",
                "www.liquibase.org/xml/ns/pro/liquibase-pro-3.9.xsd",
                "www.liquibase.org/xml/ns/pro/liquibase-pro-3.10.xsd",
                "www.liquibase.org/xml/ns/pro/liquibase-pro-4.0.xsd",
                "www.liquibase.org/xml/ns/pro/liquibase-pro-4.1.xsd",
                "liquibase.build.properties"));

        // liquibase resource bundles
        resourceBundle.produce(new NativeImageResourceBundleBuildItem("liquibase/i18n/liquibase-core"));
    }

    private void addService(BuildProducer<ServiceProviderBuildItem> services,
            BuildProducer<ReflectiveClassBuildItem> reflective, Class<?> serviceClass,
            boolean shouldRegisterFieldForReflection) {
        try {
            String service = "META-INF/services/" + serviceClass.getName();
            Set<String> implementations = ServiceUtil.classNamesNamedIn(Thread.currentThread().getContextClassLoader(),
                    service);
            services.produce(new ServiceProviderBuildItem(serviceClass.getName(), implementations.toArray(new String[0])));

            reflective.produce(new ReflectiveClassBuildItem(true, true, shouldRegisterFieldForReflection,
                    implementations.toArray(new String[0])));
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
        // add the @LiquibaseDataSource class otherwise it won't registered as a qualifier
        additionalBeans.produce(AdditionalBeanBuildItem.builder().addBeanClass(LiquibaseDataSource.class).build());

        Collection<String> dataSourceNames = getDataSourceNames(jdbcDataSourceBuildItems);

        for (String dataSourceName : dataSourceNames) {
            SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                    .configure(LiquibaseFactory.class)
                    .scope(ApplicationScoped.class) // this is what the existing code does, but it doesn't seem reasonable
                    .setRuntimeInit()
                    .unremovable()
                    .supplier(recorder.liquibaseSupplier(dataSourceName));

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
    @Consume(SyntheticBeansRuntimeInitBuildItem.class)
    ServiceStartBuildItem startLiquibase(LiquibaseRecorder recorder,
            List<JdbcDataSourceBuildItem> jdbcDataSourceBuildItems,
            BuildProducer<JdbcDataSourceSchemaReadyBuildItem> schemaReadyBuildItem) {
        // will actually run the actions at runtime
        recorder.doStartActions();

        // once we are done running the migrations, we produce a build item indicating that the
        // schema is "ready"
        schemaReadyBuildItem.produce(new JdbcDataSourceSchemaReadyBuildItem(getDataSourceNames(jdbcDataSourceBuildItems)));

        return new ServiceStartBuildItem("liquibase");
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
        ClassLoaderResourceAccessor classLoaderResourceAccessor = new ClassLoaderResourceAccessor(
                Thread.currentThread().getContextClassLoader());

        ChangeLogParserFactory changeLogParserFactory = ChangeLogParserFactory.getInstance();

        Set<String> resources = new LinkedHashSet<>();

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
