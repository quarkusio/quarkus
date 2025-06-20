package io.quarkus.liquibase.mongodb.deployment;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
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
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.liquibase.mongodb.LiquibaseMongodbFactory;
import io.quarkus.liquibase.mongodb.runtime.LiquibaseMongodbBuildTimeConfig;
import io.quarkus.liquibase.mongodb.runtime.LiquibaseMongodbRecorder;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.paths.PathFilter;
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

class LiquibaseMongodbProcessor {

    private static final Logger LOGGER = Logger.getLogger(LiquibaseMongodbProcessor.class);

    private static final ArtifactCoords LIQUIBASE_ARTIFACT = Dependency.of(
            "org.liquibase", "liquibase-core", "*");
    private static final ArtifactCoords LIQUIBASE_MONGODB_ARTIFACT = Dependency.of(
            "org.liquibase.ext", "liquibase-mongodb", "*");
    private static final PathFilter LIQUIBASE_RESOURCE_FILTER = PathFilter.forIncludes(List.of(
            "*.properties",
            "www.liquibase.org/xml/ns/dbchangelog/*.xsd"));
    private static final PathFilter LIQUIBASE_MONGODB_RESOURCE_FILTER = PathFilter.forIncludes(List.of(
            "www.liquibase.org/xml/ns/mongodb/*.xsd",
            "liquibase.parser.core.xml/*.xsd"));

    private static final DotName DATABASE_CHANGE_PROPERTY = DotName.createSimple(DatabaseChangeProperty.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.LIQUIBASE_MONGODB);
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    List<IndexDependencyBuildItem> indexLiquibase() {
        return List.of(
                new IndexDependencyBuildItem(LIQUIBASE_ARTIFACT.getGroupId(), LIQUIBASE_ARTIFACT.getArtifactId()),
                new IndexDependencyBuildItem(
                        LIQUIBASE_MONGODB_ARTIFACT.getGroupId(), LIQUIBASE_MONGODB_ARTIFACT.getArtifactId()));
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void nativeImageConfiguration(
            LiquibaseMongodbBuildTimeConfig liquibaseBuildConfig,
            CombinedIndexBuildItem combinedIndex,
            CurateOutcomeBuildItem curateOutcome,
            BuildProducer<ReflectiveClassBuildItem> reflective,
            BuildProducer<NativeImageResourceBuildItem> resource,
            BuildProducer<ServiceProviderBuildItem> services,
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitialized,
            BuildProducer<NativeImageResourceBundleBuildItem> resourceBundle) {

        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem(liquibase.diff.compare.CompareControl.class.getName()));
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem(
                liquibase.sqlgenerator.core.LockDatabaseChangeLogGenerator.class.getName()));

        reflective.produce(ReflectiveClassBuildItem.builder(
                liquibase.change.AbstractSQLChange.class.getName(),
                liquibase.ext.mongodb.change.AbstractMongoChange.class.getName(),
                liquibase.database.jvm.JdbcConnection.class.getName())
                .methods().build());

        reflective.produce(ReflectiveClassBuildItem
                .builder(combinedIndex.getIndex().getAllKnownSubclasses(AbstractPluginFactory.class).stream()
                        .map(classInfo -> classInfo.name().toString())
                        .toArray(String[]::new))
                .reason(getClass().getName())
                .constructors().build());

        reflective.produce(ReflectiveClassBuildItem.builder(
                liquibase.command.CommandFactory.class.getName(),
                liquibase.database.LiquibaseTableNamesFactory.class.getName(),
                liquibase.configuration.ConfiguredValueModifierFactory.class.getName(),
                liquibase.changelog.FastCheckService.class.getName(),
                // deprecated, but still used by liquibase.nosql.lockservice.AbstractNoSqlLockService
                liquibase.configuration.GlobalConfiguration.class.getName())
                .constructors().build());

        reflective.produce(ReflectiveClassBuildItem.builder(
                liquibase.configuration.LiquibaseConfiguration.class.getName(),
                liquibase.parser.ChangeLogParserConfiguration.class.getName(),
                liquibase.GlobalConfiguration.class.getName(),
                liquibase.executor.ExecutorService.class.getName(),
                liquibase.change.ChangeFactory.class.getName(),
                liquibase.change.ColumnConfig.class.getName(),
                liquibase.change.AddColumnConfig.class.getName(),
                liquibase.change.core.LoadDataColumnConfig.class.getName(),
                liquibase.sql.visitor.PrependSqlVisitor.class.getName(),
                liquibase.sql.visitor.ReplaceSqlVisitor.class.getName(),
                liquibase.sql.visitor.AppendSqlVisitor.class.getName(),
                liquibase.sql.visitor.RegExpReplaceSqlVisitor.class.getName(),
                liquibase.ext.mongodb.database.MongoClientDriver.class.getName())
                .constructors().methods().fields().build());

        reflective.produce(ReflectiveClassBuildItem.builder(
                liquibase.change.ConstraintsConfig.class.getName())
                .fields().build());

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
                        .reason(getClass().getName())
                        .constructors().methods().fields().build());

        resource.produce(
                new NativeImageResourceBuildItem(getChangeLogs(liquibaseBuildConfig).toArray(new String[0])));

        // Register Precondition services, and the implementation class for reflection while also registering fields for reflection
        addService(services, reflective, liquibase.precondition.Precondition.class.getName(), true);

        // CommandStep implementations are needed (just like in non-mongodb variant)
        addService(services, reflective, liquibase.command.CommandStep.class.getName(), false,
                "liquibase.command.core.StartH2CommandStep");

        var dependencies = curateOutcome.getApplicationModel().getRuntimeDependencies();

        resource.produce(NativeImageResourceBuildItem.ofDependencyResources(
                dependencies, LIQUIBASE_ARTIFACT, LIQUIBASE_RESOURCE_FILTER));
        resource.produce(NativeImageResourceBuildItem.ofDependencyResources(
                dependencies, LIQUIBASE_MONGODB_ARTIFACT, LIQUIBASE_MONGODB_RESOURCE_FILTER));
        services.produce(ServiceProviderBuildItem.allProvidersOfDependencies(
                dependencies, List.of(LIQUIBASE_ARTIFACT, LIQUIBASE_MONGODB_ARTIFACT)));

        // liquibase resource bundles
        resourceBundle.produce(new NativeImageResourceBundleBuildItem("liquibase/i18n/liquibase-core"));
        resourceBundle.produce(new NativeImageResourceBundleBuildItem("liquibase/i18n/liquibase-mongo"));
    }

    private void addService(BuildProducer<ServiceProviderBuildItem> services,
            BuildProducer<ReflectiveClassBuildItem> reflective, String serviceClassName,
            boolean shouldRegisterFieldForReflection, String... excludedImpls) {
        try {
            String service = ServiceProviderBuildItem.SPI_ROOT + serviceClassName;
            Set<String> implementations = ServiceUtil.classNamesNamedIn(Thread.currentThread().getContextClassLoader(),
                    service);
            if (excludedImpls.length > 0) {
                implementations = new HashSet<>(implementations);
                Arrays.asList(excludedImpls).forEach(implementations::remove);
            }
            services.produce(new ServiceProviderBuildItem(serviceClassName, implementations.toArray(new String[0])));

            reflective.produce(
                    ReflectiveClassBuildItem.builder(implementations.toArray(new String[0])).reason(getClass().getName())
                            .constructors().methods().fields(shouldRegisterFieldForReflection).build());
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void createBeans(LiquibaseMongodbRecorder recorder,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer) {

        SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                .configure(LiquibaseMongodbFactory.class)
                .scope(ApplicationScoped.class) // this is what the existing code does, but it doesn't seem reasonable
                .setRuntimeInit()
                .unremovable()
                .supplier(recorder.liquibaseSupplier());

        syntheticBeanBuildItemBuildProducer.produce(configurator.done());
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    @Consume(BeanContainerBuildItem.class)
    ServiceStartBuildItem startLiquibase(LiquibaseMongodbRecorder recorder,
            BuildProducer<InitTaskCompletedBuildItem> initializationCompleteBuildItem) {
        // will actually run the actions at runtime
        recorder.doStartActions();
        initializationCompleteBuildItem.produce(new InitTaskCompletedBuildItem("liquibase-mongodb"));
        return new ServiceStartBuildItem("liquibase-mongodb");
    }

    @BuildStep
    public InitTaskBuildItem configureInitTask(ApplicationInfoBuildItem app) {
        return InitTaskBuildItem.create()
                .withName(app.getName() + "-liquibase-mongodb-init")
                .withTaskEnvVars(
                        Map.of("QUARKUS_INIT_AND_EXIT", "true", "QUARKUS_LIQUIBASE_MONGODB_ENABLED", "true"))
                .withAppEnvVars(Map.of("QUARKUS_LIQUIBASE_MONGODB_ENABLED", "false"))
                .withSharedEnvironment(true)
                .withSharedFilesystem(true);
    }

    /**
     * Collect the configured changeLog file for the default and all named datasources.
     * <p>
     * A {@link LinkedHashSet} is used to avoid duplications.
     */
    private List<String> getChangeLogs(LiquibaseMongodbBuildTimeConfig liquibaseBuildConfig) {
        ChangeLogParameters changeLogParameters = new ChangeLogParameters();

        ChangeLogParserFactory changeLogParserFactory = ChangeLogParserFactory.getInstance();

        try (var classLoaderResourceAccessor = new ClassLoaderResourceAccessor(
                Thread.currentThread().getContextClassLoader())) {

            Set<String> resources = new LinkedHashSet<>(
                    findAllChangeLogFiles(liquibaseBuildConfig.changeLog(), changeLogParserFactory,
                            classLoaderResourceAccessor, changeLogParameters));

            LOGGER.debugf("Liquibase changeLogs: %s", resources);

            return new ArrayList<>(resources);

        } catch (Exception ex) {
            // close() really shouldn't declare that exception, see also https://github.com/liquibase/liquibase/pull/2576
            throw new IllegalStateException(
                    "Error while loading the liquibase changelogs: %s".formatted(ex.getMessage()), ex);
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
        if (change instanceof LoadDataChange loadDataChange) {
            path = loadDataChange.getFile();
            relative = loadDataChange.isRelativeToChangelogFile();
        } else if (change instanceof SQLFileChange sqlFileChange) {
            path = sqlFileChange.getPath();
            relative = sqlFileChange.isRelativeToChangelogFile();
        } else if (change instanceof CreateProcedureChange createProcedureChange) {
            path = createProcedureChange.getPath();
            relative = createProcedureChange.isRelativeToChangelogFile();
        } else if (change instanceof CreateViewChange createViewChange) {
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
