package io.quarkus.liquibase.mongodb.deployment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
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
import io.quarkus.liquibase.common.LiquibaseChangeLogResourceDiscovery;
import io.quarkus.liquibase.common.LiquibaseChangeLogResourceDiscovery.LogicalPhysicalAlias;
import io.quarkus.liquibase.common.runtime.LiquibaseLogicalPathMappings;
import io.quarkus.liquibase.mongodb.LiquibaseMongodbFactory;
import io.quarkus.liquibase.mongodb.runtime.LiquibaseMongodbBuildTimeClientConfig;
import io.quarkus.liquibase.mongodb.runtime.LiquibaseMongodbBuildTimeConfig;
import io.quarkus.liquibase.mongodb.runtime.LiquibaseMongodbClient;
import io.quarkus.liquibase.mongodb.runtime.LiquibaseMongodbRecorder;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.mongodb.runtime.MongoConfig;
import io.quarkus.paths.PathFilter;
import liquibase.change.DatabaseChangeProperty;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.parser.ChangeLogParser;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.plugin.AbstractPluginFactory;
import liquibase.resource.ClassLoaderResourceAccessor;

class LiquibaseMongodbProcessor {
    private static final String LIQUIBASE_MONGODB_BEAN_NAME_PREFIX = "liquibase_mongodb_";
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

    @BuildStep
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
                liquibase.precondition.PreconditionLogic.class.getName())
                .reason(getClass().getName())
                .fields().build());

        reflective.produce(ReflectiveClassBuildItem.builder(
                liquibase.command.CommandFactory.class.getName(),
                liquibase.database.LiquibaseTableNamesFactory.class.getName(),
                liquibase.configuration.ConfiguredValueModifierFactory.class.getName(),
                liquibase.changelog.FastCheckService.class.getName())
                .reason(getClass().getName())
                .constructors().build());

        reflective.produce(ReflectiveClassBuildItem.builder(
                liquibase.change.AbstractSQLChange.class.getName(),
                liquibase.ext.mongodb.change.AbstractMongoChange.class.getName(),
                liquibase.ext.mongodb.change.CreateCollectionChange.class.getName(),
                liquibase.ext.mongodb.change.CreateIndexChange.class.getName(),
                liquibase.ext.mongodb.change.DropCollectionChange.class.getName(),
                liquibase.ext.mongodb.change.DropIndexChange.class.getName(),
                liquibase.ext.mongodb.change.InsertManyChange.class.getName(),
                liquibase.ext.mongodb.change.InsertOneChange.class.getName(),
                liquibase.ext.mongodb.change.RunCommandChange.class.getName(),
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

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void liquibaseNativeLogicalPathMappings(
            LiquibaseMongodbBuildTimeConfig liquibaseBuildConfig,
            BuildProducer<GeneratedResourceBuildItem> generatedResources,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources) {

        ChangeLogParameters changeLogParameters = new ChangeLogParameters();
        ChangeLogParserFactory changeLogParserFactory = ChangeLogParserFactory.getInstance();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        LinkedHashSet<LogicalPhysicalAlias> allAliases = new LinkedHashSet<>();
        try (var classLoaderResourceAccessor = new ClassLoaderResourceAccessor(classLoader)) {
            for (LiquibaseMongodbBuildTimeClientConfig buildConfig : liquibaseBuildConfig.clientConfigs().values()) {
                String changeLog = buildConfig.changeLog();
                ChangeLogParser parser = changeLogParserFactory.getParser(changeLog, classLoaderResourceAccessor);
                DatabaseChangeLog root = parser.parse(changeLog, changeLogParameters, classLoaderResourceAccessor);
                if (root != null) {
                    allAliases.addAll(LiquibaseChangeLogResourceDiscovery.scan(root).logicalPhysicalAliases());
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Error while loading the liquibase changelogs: %s".formatted(ex.getMessage()), ex);
        }

        byte[] mappingBytes = mergeLogicalPathMappingProperties(allAliases);
        if (mappingBytes != null) {
            generatedResources.produce(
                    new GeneratedResourceBuildItem(LiquibaseLogicalPathMappings.MONGODB_MAPPING_RESOURCE, mappingBytes));
            nativeImageResources
                    .produce(new NativeImageResourceBuildItem(LiquibaseLogicalPathMappings.MONGODB_MAPPING_RESOURCE));
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
        sb.append("# Generated by Quarkus -- logicalFilePath to classpath resource path (MongoDB Liquibase)\n");
        for (var entry : sorted.entrySet()) {
            sb.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
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
            LiquibaseMongodbBuildTimeConfig config,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer) {

        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(LiquibaseMongodbClient.class).build());

        for (String clientName : config.clientConfigs().keySet()) {
            SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                    .configure(LiquibaseMongodbFactory.class)
                    .scope(ApplicationScoped.class) // this is what the existing code does, but it doesn't seem reasonable
                    .setRuntimeInit()
                    .unremovable()
                    .supplier(recorder.liquibaseSupplier(clientName));

            if (MongoConfig.isDefaultClient(clientName)) {
                configurator.addQualifier(Default.class);
            } else {
                configurator.name(LIQUIBASE_MONGODB_BEAN_NAME_PREFIX + clientName);
                configurator.addQualifier().annotation(LiquibaseMongodbClient.class)
                        .addValue("value", clientName).done();
            }

            syntheticBeanBuildItemBuildProducer.produce(configurator.done());
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    @Consume(BeanContainerBuildItem.class)
    ServiceStartBuildItem startLiquibase(LiquibaseMongodbRecorder recorder,
            LiquibaseMongodbBuildTimeConfig config,
            BuildProducer<InitTaskCompletedBuildItem> initializationCompleteBuildItem) {
        // will actually run the actions at runtime
        for (String clientName : config.clientConfigs().keySet()) {
            recorder.doStartActions(clientName);
        }
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
     * Collect the configured changeLog file for the default and all named clients.
     * <p>
     * A {@link LinkedHashSet} is used to avoid duplications.
     */
    private List<String> getChangeLogs(LiquibaseMongodbBuildTimeConfig liquibaseBuildConfig) {
        ChangeLogParameters changeLogParameters = new ChangeLogParameters();

        ChangeLogParserFactory changeLogParserFactory = ChangeLogParserFactory.getInstance();

        try (var classLoaderResourceAccessor = new ClassLoaderResourceAccessor(
                Thread.currentThread().getContextClassLoader())) {

            Set<String> resources = new LinkedHashSet<>();
            for (LiquibaseMongodbBuildTimeClientConfig buildConfig : liquibaseBuildConfig.clientConfigs().values()) {
                ChangeLogParser parser = changeLogParserFactory.getParser(buildConfig.changeLog(),
                        classLoaderResourceAccessor);
                DatabaseChangeLog changelog = parser.parse(buildConfig.changeLog(), changeLogParameters,
                        classLoaderResourceAccessor);
                if (changelog != null) {
                    resources.addAll(LiquibaseChangeLogResourceDiscovery.scan(changelog).resourcePaths());
                }
            }
            LOGGER.debugf("Liquibase changeLogs: %s", resources);

            return new ArrayList<>(resources);

        } catch (Exception ex) {
            // close() really shouldn't declare that exception, see also https://github.com/liquibase/liquibase/pull/2576
            throw new IllegalStateException(
                    "Error while loading the liquibase changelogs: %s".formatted(ex.getMessage()), ex);
        }
    }

}
