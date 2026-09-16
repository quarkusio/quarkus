package io.quarkus.liquibase.deployment;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

import io.quarkus.agroal.deployment.AgroalDataSourceBuildUtil;
import io.quarkus.agroal.spi.JdbcDataSourceBuildItem;
import io.quarkus.agroal.spi.JdbcDataSourceSchemaReadyBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.deployment.spi.component.DataSourceRequestBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
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
import io.quarkus.liquibase.common.LiquibaseChangeLogResourceDiscovery;
import io.quarkus.liquibase.common.LiquibaseChangeLogResourceDiscovery.LogicalPhysicalAlias;
import io.quarkus.liquibase.common.runtime.LiquibaseLogicalPathMappings;
import io.quarkus.liquibase.runtime.LiquibaseBuildTimeConfig;
import io.quarkus.liquibase.runtime.LiquibaseDataSourceBuildTimeConfig;
import io.quarkus.liquibase.runtime.LiquibaseFactoryProducer;
import io.quarkus.liquibase.runtime.LiquibaseFactoryUtil;
import io.quarkus.liquibase.runtime.LiquibaseRecorder;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.paths.PathFilter;
import io.quarkus.runtime.util.ProgrammingParadigm;
import liquibase.change.DatabaseChangeProperty;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.datatype.DataTypeInfo;
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
    private static final ArtifactCoords LIQUIBASE_ARTIFACT = Dependency.of("org.liquibase", "liquibase-core", "*");
    private static final PathFilter LIQUIBASE_RESOURCE_FILTER = PathFilter.forIncludes(List.of(
            "*.properties",
            "www.liquibase.org/xml/ns/dbchangelog/*.xsd"));

    private static final DotName DATABASE_CHANGE_PROPERTY = DotName.createSimple(DatabaseChangeProperty.class.getName());
    private static final DotName DATA_TYPE_INFO_ANNOTATION = DotName.createSimple(DataTypeInfo.class.getName());

    @BuildStep
    void collectImplicitDataSourceRequestsFromConfiguration(
            LiquibaseBuildTimeConfig liquibaseBuildTimeConfig,
            BuildProducer<DataSourceRequestBuildItem> dataSourceRequests) {
        for (String dsName : liquibaseBuildTimeConfig.datasources().keySet()) {
            dataSourceRequests.produce(new DataSourceRequestBuildItem(dsName, ProgrammingParadigm.BLOCKING,
                    String.format("Configuration '%s'", LiquibaseFactoryUtil.liquibasePropertyKey(dsName, "*"))));
        }

        // We don't derive requests from injection points of datasource related beans,
        // because those could just be referencing custom beans,
        // as we suggest in https://quarkus.io/guides/datasource#datasource-active
        // TODO https://github.com/quarkusio/quarkus/issues/55217
        //  Find a way to collect injection points for a given DS that have no matching user-defined producer
    }

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.LIQUIBASE);
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    IndexDependencyBuildItem indexLiquibase() {
        return new IndexDependencyBuildItem(LIQUIBASE_ARTIFACT.getGroupId(), LIQUIBASE_ARTIFACT.getArtifactId());
    }

    @BuildStep
    void nativeImageConfiguration(
            LiquibaseBuildTimeConfig liquibaseBuildConfig,
            List<JdbcDataSourceBuildItem> jdbcDataSourceBuildItems,
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

        reflective.produce(ReflectiveClassBuildItem
                .builder(liquibase.datatype.core.UnknownType.class.getName())
                .reason(getClass().getName())
                .constructors().methods()
                .build());

        reflective.produce(ReflectiveClassBuildItem.builder(
                liquibase.AbstractExtensibleObject.class.getName(),
                liquibase.change.core.DeleteDataChange.class.getName(),
                liquibase.change.core.EmptyChange.class.getName(),
                liquibase.database.jvm.JdbcConnection.class.getName(),
                liquibase.plugin.AbstractPlugin.class.getName())
                .reason(getClass().getName())
                .methods()
                .build());

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
                liquibase.changelog.FastCheckService.class.getName())
                .reason(getClass().getName())
                .constructors().build());

        reflective.produce(ReflectiveClassBuildItem.builder(
                liquibase.changelog.RanChangeSet.class.getName(),
                liquibase.configuration.LiquibaseConfiguration.class.getName(),
                liquibase.parser.ChangeLogParserConfiguration.class.getName(),
                liquibase.GlobalConfiguration.class.getName(),
                liquibase.executor.ExecutorService.class.getName(),
                liquibase.change.ColumnConfig.class.getName(),
                liquibase.change.AddColumnConfig.class.getName(),
                liquibase.change.core.LoadDataColumnConfig.class.getName())
                .reason(getClass().getName())
                .constructors().methods().fields().build());

        reflective.produce(ReflectiveClassBuildItem.builder(
                liquibase.change.ConstraintsConfig.class.getName())
                .reason(getClass().getName())
                .fields().build());

        // liquibase seems to instantiate these types reflectively...
        reflective.produce(ReflectiveClassBuildItem.builder(ConcurrentHashMap.class, ArrayList.class)
                .reason(getClass().getName())
                .build());

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

        // the subclasses of AbstractSqlVisitor are also accessed reflectively
        reflective.produce(ReflectiveClassBuildItem.builder(
                liquibase.sql.visitor.AbstractSqlVisitor.class.getName(),
                liquibase.sql.visitor.AppendSqlIfNotPresentVisitor.class.getName(),
                liquibase.sql.visitor.AppendSqlVisitor.class.getName(),
                liquibase.sql.visitor.PrependSqlVisitor.class.getName(),
                liquibase.sql.visitor.RegExpReplaceSqlVisitor.class.getName(),
                liquibase.sql.visitor.ReplaceSqlVisitor.class.getName())
                .reason(getClass().getName())
                .constructors().methods().fields().build());

        // register all liquibase.datatype.core.* data types
        Set<String> classesAnnotatedWithDataTypeInfo = combinedIndex.getIndex().getAnnotations(DATA_TYPE_INFO_ANNOTATION)
                .stream()
                .map(AnnotationInstance::target)
                .filter(at -> at.kind() == AnnotationTarget.Kind.CLASS)
                .map(at -> at.asClass().name().toString())
                .collect(Collectors.toSet());
        reflective.produce(ReflectiveClassBuildItem.builder(classesAnnotatedWithDataTypeInfo.toArray(String[]::new))
                .reason(getClass().getName())
                .constructors().methods()
                .build());

        Collection<String> dataSourceNames = jdbcDataSourceBuildItems.stream()
                .map(JdbcDataSourceBuildItem::getName)
                .collect(Collectors.toSet());

        resource.produce(
                new NativeImageResourceBuildItem(getChangeLogs(dataSourceNames, liquibaseBuildConfig).toArray(new String[0])));

        // Register Precondition services, and the implementation class for reflection while also registering fields for reflection
        consumeService(liquibase.precondition.Precondition.class.getName(), (serviceClassName, implementations) -> {
            services.produce(new ServiceProviderBuildItem(serviceClassName, implementations.toArray(new String[0])));
            reflective.produce(ReflectiveClassBuildItem.builder(implementations.toArray(new String[0]))
                    .reason(getClass().getName())
                    .constructors().methods().fields().build());
        });

        reflective.produce(ReflectiveClassBuildItem.builder(
                liquibase.precondition.PreconditionLogic.class.getName())
                .reason(getClass().getName())
                .fields().build());

        var dependencies = curateOutcome.getApplicationModel().getRuntimeDependencies();
        resource.produce(NativeImageResourceBuildItem.ofDependencyResources(
                dependencies, LIQUIBASE_ARTIFACT, LIQUIBASE_RESOURCE_FILTER));
        services.produce(ServiceProviderBuildItem.allProvidersOfDependency(dependencies, LIQUIBASE_ARTIFACT));

        // liquibase resource bundles
        resourceBundle.produce(new NativeImageResourceBundleBuildItem("liquibase/i18n/liquibase-core"));
    }

    /**
     * Writes a small logical→physical classpath mapping for Liquibase {@code logicalFilePath} entries so native
     * image can resolve changelog paths without duplicating file contents.
     */
    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void liquibaseNativeLogicalPathMappings(
            LiquibaseBuildTimeConfig liquibaseBuildConfig,
            List<JdbcDataSourceBuildItem> jdbcDataSourceBuildItems,
            BuildProducer<GeneratedResourceBuildItem> generatedResources,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources) {

        Collection<String> dataSourceNames = jdbcDataSourceBuildItems.stream()
                .map(JdbcDataSourceBuildItem::getName)
                .collect(Collectors.toSet());

        if (dataSourceNames.isEmpty()) {
            return;
        }

        List<LiquibaseDataSourceBuildTimeConfig> liquibaseDataSources = new ArrayList<>();
        for (String dataSourceName : dataSourceNames) {
            liquibaseDataSources.add(liquibaseBuildConfig.datasources().get(dataSourceName));
        }

        LinkedHashSet<LogicalPhysicalAlias> allAliases = new LinkedHashSet<>();
        forEachChangeLog(liquibaseDataSources, changelog -> allAliases.addAll(
                LiquibaseChangeLogResourceDiscovery.scan(changelog).logicalPhysicalAliases()));

        byte[] mappingBytes = mergeLogicalPathMappingProperties(allAliases);
        if (mappingBytes != null) {
            generatedResources.produce(
                    new GeneratedResourceBuildItem(LiquibaseLogicalPathMappings.JDBC_MAPPING_RESOURCE, mappingBytes));
            nativeImageResources.produce(new NativeImageResourceBuildItem(LiquibaseLogicalPathMappings.JDBC_MAPPING_RESOURCE));
        }
    }

    private byte[] mergeLogicalPathMappingProperties(LinkedHashSet<LogicalPhysicalAlias> aliases) {
        if (aliases.isEmpty()) {
            return null;
        }
        TreeMap<String, String> sorted = new TreeMap<>();
        for (LogicalPhysicalAlias alias : aliases) {
            String previous = sorted.put(alias.logical(), alias.physical());
            if (previous != null && !previous.equals(alias.physical())) {
                LOGGER.warnf("Conflicting Liquibase logical path mapping for %s: %s vs %s", alias.logical(), previous,
                        alias.physical());
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("# Generated by Quarkus -- logicalFilePath to classpath resource path\n");
        for (var entry : sorted.entrySet()) {
            sb.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void consumeService(String serviceClassName, BiConsumer<String, Collection<String>> consumer) {
        try {
            String service = ServiceProviderBuildItem.SPI_ROOT + serviceClassName;
            Set<String> implementations = ServiceUtil.classNamesNamedIn(Thread.currentThread().getContextClassLoader(),
                    service);
            consumer.accept(serviceClassName, implementations);
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
                    .addInjectionPoint(ClassType.create(DotName.createSimple(DataSource.class)),
                            AgroalDataSourceBuildUtil.qualifier(dataSourceName))
                    .startup()
                    .checkActive(recorder.liquibaseCheckActiveSupplier(dataSourceName))
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

        for (String dataSourceName : dataSourceNames) {
            liquibaseDataSources.add(liquibaseBuildConfig.datasources().get(dataSourceName));
        }

        Set<String> resources = new LinkedHashSet<>();
        forEachChangeLog(liquibaseDataSources, changelog -> resources.addAll(
                LiquibaseChangeLogResourceDiscovery.scan(changelog).resourcePaths()));

        LOGGER.debugf("Liquibase changeLogs: %s", resources);
        return new ArrayList<>(resources);
    }

    /**
     * Resolves and parses the change log for each datasource configuration, passing each
     * successfully parsed {@link DatabaseChangeLog} to the given consumer. Configurations
     * whose change log cannot be found are silently skipped.
     *
     * @param configs the datasource build-time configurations to process
     * @param consumer receives each successfully parsed {@link DatabaseChangeLog}
     */
    private static void forEachChangeLog(List<LiquibaseDataSourceBuildTimeConfig> configs,
            Consumer<DatabaseChangeLog> consumer) {
        ChangeLogParameters changeLogParameters = new ChangeLogParameters();
        ChangeLogParserFactory changeLogParserFactory = ChangeLogParserFactory.getInstance();
        try (var classpathAccessor = new ClassLoaderResourceAccessor(Thread.currentThread().getContextClassLoader())) {
            for (LiquibaseDataSourceBuildTimeConfig config : configs) {
                ResolvedChangeLog resolved = resolveChangeLog(config.changeLog(), config.searchPath(),
                        classpathAccessor);
                if (resolved == null) {
                    LOGGER.debugf("Liquibase changeLog '%s' not found, skipping", config.changeLog());
                    continue;
                }
                try (resolved) {
                    ChangeLogParser parser = changeLogParserFactory.getParser(resolved.path(),
                            resolved.resourceAccessor());
                    DatabaseChangeLog changelog = parser.parse(resolved.path(), changeLogParameters,
                            resolved.resourceAccessor());
                    if (changelog != null) {
                        consumer.accept(changelog);
                    }
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * A resolved change log with the path to pass to the Liquibase parser and the
     * {@link ResourceAccessor} to use for reading it. Implements {@link AutoCloseable}
     * so it can be used in try-with-resources; only closes the accessor when it owns it.
     */
    private record ResolvedChangeLog(String path, ResourceAccessor resourceAccessor, boolean ownsAccessor)
            implements
                AutoCloseable {
        @Override
        public void close() throws Exception {
            if (ownsAccessor) {
                resourceAccessor.close();
            }
        }
    }

    /**
     * Resolves the configured change log to the path and {@link ResourceAccessor} that should be
     * passed to the Liquibase parser, returning {@code null} if the resource cannot be found.
     * <p>
     * {@code filesystem:} paths are resolved against the file system (using search paths if configured),
     * while classpath resources (with or without the {@code classpath:} prefix) are looked up on the
     * runtime classpath via {@link QuarkusClassLoader#isResourcePresentAtRuntime(String)}.
     *
     * @param changeLog the configured change log path, possibly prefixed with {@code filesystem:} or {@code classpath:}
     * @param oSearchPaths optional search paths checked for both {@code filesystem:} and unprefixed change logs
     * @param classpathAccessor shared classpath accessor reused for classpath-only change logs;
     *        {@code filesystem:} and search-path cases create their own accessor
     * @return the resolved change log, or {@code null} if the change log was not found
     */
    private static ResolvedChangeLog resolveChangeLog(String changeLog, Optional<List<String>> oSearchPaths,
            ClassLoaderResourceAccessor classpathAccessor)
            throws FileNotFoundException {
        boolean filesystemOnly = changeLog.startsWith("filesystem:");

        if (filesystemOnly) {
            changeLog = changeLog.substring("filesystem:".length());
        } else {
            if (changeLog.startsWith("classpath:")) {
                changeLog = changeLog.substring("classpath:".length());
            }
            if (QuarkusClassLoader.isResourcePresentAtRuntime(changeLog)) {
                return new ResolvedChangeLog(changeLog, classpathAccessor, false);
            }
        }

        if (oSearchPaths.isPresent()) {
            for (String sp : oSearchPaths.get()) {
                if (Files.exists(Path.of(sp).resolve(changeLog))) {
                    CompositeResourceAccessor accessor = new CompositeResourceAccessor();
                    accessor.addResourceAccessor(
                            new ClassLoaderResourceAccessor(Thread.currentThread().getContextClassLoader()));
                    for (String searchPath : oSearchPaths.get()) {
                        accessor.addResourceAccessor(new DirectoryResourceAccessor(Path.of(searchPath)));
                    }
                    return new ResolvedChangeLog(changeLog, accessor, true);
                }
            }
        } else if (filesystemOnly) {
            Path path = Path.of(changeLog);
            if (Files.exists(path)) {
                CompositeResourceAccessor accessor = new CompositeResourceAccessor();
                accessor.addResourceAccessor(
                        new ClassLoaderResourceAccessor(Thread.currentThread().getContextClassLoader()));
                accessor.addResourceAccessor(new DirectoryResourceAccessor(path.getParent()));
                return new ResolvedChangeLog(path.getFileName().toString(), accessor, true);
            }
        }

        return null;
    }

}
