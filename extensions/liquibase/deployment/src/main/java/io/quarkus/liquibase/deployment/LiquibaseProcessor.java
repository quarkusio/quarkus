package io.quarkus.liquibase.deployment;

import static java.util.function.Predicate.not;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
import io.quarkus.deployment.Capabilities;
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
import io.quarkus.liquibase.LiquibaseDataSource;
import io.quarkus.liquibase.LiquibaseFactory;
import io.quarkus.liquibase.runtime.LiquibaseBuildTimeConfig;
import io.quarkus.liquibase.runtime.LiquibaseDataSourceBuildTimeConfig;
import io.quarkus.liquibase.runtime.LiquibaseFactoryProducer;
import io.quarkus.liquibase.runtime.LiquibaseRecorder;
import io.quarkus.paths.PathTree;
import io.quarkus.runtime.util.StringUtil;
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
import liquibase.resource.CompositeResourceAccessor;
import liquibase.resource.DirectoryResourceAccessor;
import liquibase.resource.ResourceAccessor;

class LiquibaseProcessor {

    private static final Logger LOGGER = Logger.getLogger(LiquibaseProcessor.class);

    private static final String LIQUIBASE_BEAN_NAME_PREFIX = "liquibase_";
    private static final String LIQUIBASE_GROUP_ID = "org.liquibase";
    private static final String LIQUIBASE_ARTIFACT_ID = "liquibase-core";
    private static final String LIQUIBASE_PROPERTIES_PATH = "";
    private static final String LIQUIBASE_DB_CHANGELOG_XSD_PATH = "www.liquibase.org/xml/ns/dbchangelog";
    private static final String LIQUIBASE_SERVICE_PATH = "META-INF/services/";

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
    void nativeImageConfiguration(
            LiquibaseBuildTimeConfig liquibaseBuildConfig,
            List<JdbcDataSourceBuildItem> jdbcDataSourceBuildItems,
            CombinedIndexBuildItem combinedIndex,
            CurateOutcomeBuildItem curateOutcome,
            Capabilities capabilities,
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

        reflective.produce(ReflectiveClassBuildItem.builder(
                liquibase.command.CommandFactory.class.getName(),
                liquibase.database.LiquibaseTableNamesFactory.class.getName(),
                liquibase.configuration.ConfiguredValueModifierFactory.class.getName())
                .constructors().build());

        reflective.produce(ReflectiveClassBuildItem.builder(
                liquibase.configuration.LiquibaseConfiguration.class.getName(),
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

        // Register Precondition services, and the implementation class for reflection while also registering fields for reflection
        consumeService(liquibase.precondition.Precondition.class.getName(), (serviceClassName, implementations) -> {
            services.produce(new ServiceProviderBuildItem(serviceClassName, implementations.toArray(new String[0])));
            reflective.produce(ReflectiveClassBuildItem.builder(implementations.toArray(new String[0]))
                    .constructors().methods().fields().build());
        });

        // CommandStep implementations are needed
        consumeService(liquibase.command.CommandStep.class.getName(), (serviceClassName, implementations) -> {
            var filteredImpls = implementations.stream()
                    .filter(commandStepPredicate(capabilities))
                    .toArray(String[]::new);
            services.produce(new ServiceProviderBuildItem(serviceClassName, filteredImpls));
            reflective.produce(ReflectiveClassBuildItem.builder(filteredImpls).constructors().build());
            for (String implementation : filteredImpls) {
                runtimeInitialized.produce(new RuntimeInitializedClassBuildItem(implementation));
            }
        });

        resolveLiquibaseResources(curateOutcome, services, reflective, resource);

        // liquibase resource bundles
        resourceBundle.produce(new NativeImageResourceBundleBuildItem("liquibase/i18n/liquibase-core"));
    }

    private static Predicate<String> commandStepPredicate(Capabilities capabilities) {
        if (capabilities.isPresent("io.quarkus.jdbc.h2")) {
            return (s) -> true;
        } else {
            return not("liquibase.command.core.StartH2CommandStep"::equals);
        }
    }

    private void consumeService(String serviceClassName, BiConsumer<String, Collection<String>> consumer) {
        try {
            String service = LIQUIBASE_SERVICE_PATH + serviceClassName;
            Set<String> implementations = ServiceUtil.classNamesNamedIn(Thread.currentThread().getContextClassLoader(),
                    service);
            consumer.accept(serviceClassName, implementations);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void resolveLiquibaseResources(
            CurateOutcomeBuildItem curateOutcome,
            BuildProducer<ServiceProviderBuildItem> services,
            BuildProducer<ReflectiveClassBuildItem> reflective,
            BuildProducer<NativeImageResourceBuildItem> resource) {

        var dependencies = curateOutcome.getApplicationModel().getDependencies();
        var liquibaseDependency = dependencies.stream()
                .filter(d -> LIQUIBASE_GROUP_ID.equals(d.getGroupId())
                        && LIQUIBASE_ARTIFACT_ID.equals(d.getArtifactId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Liquibase dependency not found"));

        var tree = liquibaseDependency.getContentTree();
        loadLiquibaseRootProperties(tree, resource);
        loadLiquibaseXsdResources(tree, resource);
        loadLiquibaseServiceProviderConfig(tree, services, reflective);
    }

    private List<String> getResourceNames(PathTree pathTree, String basePath, String fileExtension, boolean stripPath) {
        return pathTree.apply(basePath, visit -> {
            try (var pathStream = Files.list(visit.getPath())) {
                return pathStream
                        .map(p -> stripPath ? p.getFileName() : p.subpath(0, p.getNameCount()))
                        .map(Path::toString)
                        .filter(s -> s.endsWith(fileExtension))
                        .toList();
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        });
    }

    private void loadLiquibaseRootProperties(
            PathTree tree,
            BuildProducer<NativeImageResourceBuildItem> resource) {

        var rootProperties = getResourceNames(
                tree,
                LIQUIBASE_PROPERTIES_PATH,
                ".properties",
                true);
        resource.produce(new NativeImageResourceBuildItem(rootProperties));
    }

    private void loadLiquibaseXsdResources(
            PathTree tree,
            BuildProducer<NativeImageResourceBuildItem> resource) {

        var xsdResources = getResourceNames(
                tree,
                LIQUIBASE_DB_CHANGELOG_XSD_PATH,
                ".xsd",
                false);
        resource.produce(new NativeImageResourceBuildItem(xsdResources));
    }

    private void loadLiquibaseServiceProviderConfig(
            PathTree tree,
            BuildProducer<ServiceProviderBuildItem> services,
            BuildProducer<ReflectiveClassBuildItem> reflective) {

        getResourceNames(tree, LIQUIBASE_SERVICE_PATH, "", true)
                .forEach(t -> consumeService(t, (serviceClassName, implementations) -> {
                    services.produce(new ServiceProviderBuildItem(
                            serviceClassName,
                            implementations.toArray(String[]::new)));
                    reflective.produce(ReflectiveClassBuildItem.builder(implementations.toArray(String[]::new))
                            .constructors().methods().build());
                }));
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

        List<LiquibaseDataSourceBuildTimeConfig> liquibaseDataSources = new ArrayList<>();

        if (DataSourceUtil.hasDefault(dataSourceNames)) {
            liquibaseDataSources.add(liquibaseBuildConfig.defaultDataSource);
        }

        for (String dataSourceName : dataSourceNames) {
            if (!DataSourceUtil.isDefault(dataSourceName)) {
                liquibaseDataSources.add(liquibaseBuildConfig.getConfigForDataSourceName(dataSourceName));
            }
        }

        ChangeLogParameters changeLogParameters = new ChangeLogParameters();
        ChangeLogParserFactory changeLogParserFactory = ChangeLogParserFactory.getInstance();
        Set<String> resources = new LinkedHashSet<>();
        for (LiquibaseDataSourceBuildTimeConfig liquibaseDataSourceConfig : liquibaseDataSources) {

            Optional<List<String>> oSearchPaths = liquibaseDataSourceConfig.searchPath;
            String changeLog = liquibaseDataSourceConfig.changeLog;
            String parsedChangeLog = parseChangeLog(oSearchPaths, changeLog);

            try (ResourceAccessor resourceAccessor = resolveResourceAccessor(oSearchPaths, changeLog)) {
                resources.addAll(findAllChangeLogFiles(parsedChangeLog, changeLogParserFactory,
                        resourceAccessor, changeLogParameters));
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }

        LOGGER.debugf("Liquibase changeLogs: %s", resources);
        return new ArrayList<>(resources);
    }

    private ResourceAccessor resolveResourceAccessor(Optional<List<String>> oSearchPaths, String changeLog)
            throws FileNotFoundException {

        CompositeResourceAccessor compositeResourceAccessor = new CompositeResourceAccessor();
        compositeResourceAccessor
                .addResourceAccessor(new ClassLoaderResourceAccessor(Thread.currentThread().getContextClassLoader()));

        if (!changeLog.startsWith("filesystem:") && oSearchPaths.isEmpty()) {
            return compositeResourceAccessor;
        }

        if (oSearchPaths.isEmpty()) {
            compositeResourceAccessor.addResourceAccessor(
                    new DirectoryResourceAccessor(
                            Paths.get(StringUtil.changePrefix(changeLog, "filesystem:", "")).getParent()));
            return compositeResourceAccessor;
        }

        for (String searchPath : oSearchPaths.get()) {
            compositeResourceAccessor.addResourceAccessor(new DirectoryResourceAccessor(Paths.get(searchPath)));
        }

        return compositeResourceAccessor;
    }

    private String parseChangeLog(Optional<List<String>> oSearchPaths, String changeLog) {

        if (changeLog.startsWith("filesystem:") && oSearchPaths.isEmpty()) {
            return Paths.get(StringUtil.changePrefix(changeLog, "filesystem:", "")).getFileName().toString();
        }

        if (changeLog.startsWith("filesystem:")) {
            return StringUtil.changePrefix(changeLog, "filesystem:", "");
        }

        if (changeLog.startsWith("classpath:")) {
            return StringUtil.changePrefix(changeLog, "classpath:", "");
        }

        return changeLog;
    }

    /**
     * Finds all resource files for the given change log file
     */
    private Set<String> findAllChangeLogFiles(String file, ChangeLogParserFactory changeLogParserFactory,
            ResourceAccessor resourceAccessor, ChangeLogParameters changeLogParameters) {
        try {
            ChangeLogParser parser = changeLogParserFactory.getParser(file, resourceAccessor);
            DatabaseChangeLog changelog = parser.parse(file, changeLogParameters, resourceAccessor);

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
