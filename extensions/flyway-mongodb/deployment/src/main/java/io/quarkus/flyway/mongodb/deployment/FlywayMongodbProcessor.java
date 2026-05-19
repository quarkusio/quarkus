package io.quarkus.flyway.mongodb.deployment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.inject.Default;
import jakarta.inject.Singleton;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.extensibility.ConfigurationExtension;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeansRuntimeInitBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.InitTaskBuildItem;
import io.quarkus.deployment.builditem.InitTaskCompletedBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.flyway.mongodb.FlywayMongodbClient;
import io.quarkus.flyway.mongodb.runtime.FlywayMongodbBuildTimeConfig;
import io.quarkus.flyway.mongodb.runtime.FlywayMongodbClientBuildTimeConfig;
import io.quarkus.flyway.mongodb.runtime.FlywayMongodbContainer;
import io.quarkus.flyway.mongodb.runtime.FlywayMongodbContainerProducer;
import io.quarkus.flyway.mongodb.runtime.FlywayMongodbRecorder;
import io.quarkus.mongodb.deployment.MongoUnremovableClientsBuildItem;
import io.quarkus.mongodb.deployment.spi.MongoClientBuildItem;
import io.quarkus.mongodb.deployment.spi.MongoClientSchemaReadyBuildItem;
import io.quarkus.mongodb.deployment.spi.MongoClientsBuildItem;
import io.quarkus.mongodb.runtime.MongoConfig;

@BuildSteps(onlyIf = FlywayMongodbEnabled.class)
public class FlywayMongodbProcessor {

    private static final String FLYWAY_MONGODB_CONTAINER_BEAN_NAME_PREFIX = "flyway_mongodb_container_";
    private static final String FLYWAY_MONGODB_BEAN_NAME_PREFIX = "flyway_mongodb_";

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    MigrationStateBuildItem build(FlywayMongodbRecorder recorder,
            FlywayMongodbBuildTimeConfig buildTimeConfig,
            MongoClientsBuildItem mongoClients,
            BuildProducer<NativeImageResourceBuildItem> nativeResources,
            BuildProducer<HotDeploymentWatchedFileBuildItem> watchedFiles) throws Exception {

        Map<String, Collection<String>> filesByClient = new HashMap<>();
        Map<String, MigrationStateBuildItem.MigrationState> states = new HashMap<>();

        for (MongoClientBuildItem mongoClient : mongoClients.getMongoClients()) {
            String clientName = mongoClient.getName();
            FlywayMongodbClientBuildTimeConfig clientConfig = buildTimeConfig.clients().get(clientName);
            if (clientConfig == null) {
                continue;
            }

            List<String> locations = clientConfig.locations();
            List<String> suffixes = clientConfig.migrationSuffixes();
            Set<String> resourcesLocations = new LinkedHashSet<>();
            Set<String> migrationFiles = MigrationDiscovery.findMigrations(locations, suffixes, watchedFiles,
                    resourcesLocations);

            for (String file : migrationFiles) {
                nativeResources.produce(new NativeImageResourceBuildItem(file));
            }

            filesByClient.put(clientName, migrationFiles);
            states.put(clientName, new MigrationStateBuildItem.MigrationState(resourcesLocations, migrationFiles));
        }

        recorder.setApplicationMigrationFiles(filesByClient);
        return new MigrationStateBuildItem(states);
    }

    @BuildStep
    MongoUnremovableClientsBuildItem keepMongoClientBeans() {
        return new MongoUnremovableClientsBuildItem();
    }

    /**
     * Rewrite Flyway's {@code ClasspathSqlMigrationScanner.scan(...)} so it returns the
     * build-time discovered migrations instead of calling
     * {@code Thread.currentThread().getContextClassLoader().getResource(".")}, which is
     * {@code null} under the Quarkus runtime classloader (dev mode and fast-jar prod).
     */
    @BuildStep
    BytecodeTransformerBuildItem replaceClasspathScanner() {
        return new BytecodeTransformerBuildItem(
                ClasspathSqlMigrationScannerEnhancer.TARGET_CLASS_INTERNAL.replace('/', '.'),
                new ClasspathSqlMigrationScannerEnhancer());
    }

    @BuildStep
    AdditionalBeanBuildItem registerBeans() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(FlywayMongodbContainerProducer.class)
                .addBeanClass(FlywayMongodbClient.class)
                .setUnremovable()
                .setDefaultScope(DotNames.SINGLETON)
                .build();
    }

    @BuildStep
    void nativeImageReflection(CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy) {
        // ConfigurationExtension is used by Flyway via reflection to copy configuration between instances.
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(ConfigurationExtension.class)
                .reason(getClass().getName())
                .fields().methods().build());

        // For each ConfigurationExtension implementor found in the index, register the class and any
        // nested Model types it references (needed for Flyway's ConfigurationExtension#copy()).
        for (ClassInfo ext : combinedIndex.getIndex().getAllKnownImplementors(ConfigurationExtension.class)) {
            reflectiveHierarchy.produce(ReflectiveHierarchyBuildItem
                    .builder(ext.name())
                    .ignoreMethodPredicate(m -> !ext.name().equals(m.declaringClass().name()))
                    .ignoreFieldPredicate(f -> !ext.name().equals(f.declaringClass().name()))
                    .build());
        }

    }

    @BuildStep
    @Produce(SyntheticBeansRuntimeInitBuildItem.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void createBeans(FlywayMongodbRecorder recorder,
            MigrationStateBuildItem migrationState,
            MongoClientsBuildItem mongoClients,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {

        for (MongoClientBuildItem mongoClient : mongoClients.getMongoClients()) {
            String clientName = mongoClient.getName();
            MigrationStateBuildItem.MigrationState clientState = migrationState.state
                    .getOrDefault(clientName, new MigrationStateBuildItem.MigrationState(Set.of(), Set.of()));
            boolean hasMigrations = !clientState.migrations().isEmpty();
            Set<String> resourcesLocations = clientState.resourcesLocations();

            AnnotationInstance containerQualifier;

            SyntheticBeanBuildItem.ExtendedBeanConfigurator containerBean = SyntheticBeanBuildItem
                    .configure(FlywayMongodbContainer.class)
                    .scope(Singleton.class)
                    .setRuntimeInit()
                    .unremovable()
                    .addInjectionPoint(ClassType.create(DotName.createSimple(FlywayMongodbContainerProducer.class)))
                    .startup()
                    .checkActive(recorder.flywayCheckActiveSupplier(clientName))
                    .createWith(recorder.flywayMongodbContainerFunction(clientName, hasMigrations, resourcesLocations));

            SyntheticBeanBuildItem.ExtendedBeanConfigurator flywayBean = SyntheticBeanBuildItem
                    .configure(Flyway.class)
                    .scope(Singleton.class)
                    .setRuntimeInit()
                    .unremovable()
                    .startup()
                    .checkActive(recorder.flywayCheckActiveSupplier(clientName))
                    .createWith(recorder.flywayFunction(clientName));

            if (MongoConfig.isDefaultClient(clientName)) {
                containerBean.addQualifier(Default.class);
                containerBean.priority(10);
                flywayBean.addQualifier(Default.class);
                flywayBean.priority(10);
                containerQualifier = AnnotationInstance.builder(Default.class).build();
            } else {
                String containerBeanName = FLYWAY_MONGODB_CONTAINER_BEAN_NAME_PREFIX + clientName;
                containerBean.name(containerBeanName);
                containerBean.addQualifier().annotation(DotNames.NAMED).addValue("value", containerBeanName).done();
                containerBean.addQualifier()
                        .annotation(FlywayMongodbClient.class)
                        .addValue("value", clientName)
                        .done();
                containerBean.priority(5);

                String flywayBeanName = FLYWAY_MONGODB_BEAN_NAME_PREFIX + clientName;
                flywayBean.name(flywayBeanName);
                flywayBean.addQualifier().annotation(DotNames.NAMED).addValue("value", flywayBeanName).done();
                flywayBean.addQualifier()
                        .annotation(FlywayMongodbClient.class)
                        .addValue("value", clientName)
                        .done();
                flywayBean.priority(5);
                containerQualifier = AnnotationInstance.builder(FlywayMongodbClient.class)
                        .add("value", clientName)
                        .build();
            }

            flywayBean.addInjectionPoint(
                    ClassType.create(DotName.createSimple(FlywayMongodbContainer.class)),
                    containerQualifier);

            syntheticBeans.produce(containerBean.done());
            syntheticBeans.produce(flywayBean.done());
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    @Consume(SyntheticBeansRuntimeInitBuildItem.class)
    ServiceStartBuildItem startActions(FlywayMongodbRecorder recorder,
            MigrationStateBuildItem migrationState,
            MongoClientsBuildItem mongoClients,
            BuildProducer<InitTaskCompletedBuildItem> initTaskCompleted,
            BuildProducer<MongoClientSchemaReadyBuildItem> schemaReady) {
        List<String> clientsWithMigrations = new ArrayList<>();
        for (MongoClientBuildItem mongoClient : mongoClients.getMongoClients()) {
            String clientName = mongoClient.getName();
            recorder.doStartActions(clientName);
            MigrationStateBuildItem.MigrationState clientState = migrationState.state
                    .getOrDefault(clientName, new MigrationStateBuildItem.MigrationState(Set.of(), Set.of()));
            if (!clientState.migrations().isEmpty()) {
                clientsWithMigrations.add(clientName);
            }
        }
        initTaskCompleted.produce(new InitTaskCompletedBuildItem("flyway-mongodb"));
        schemaReady.produce(new MongoClientSchemaReadyBuildItem(clientsWithMigrations));
        return new ServiceStartBuildItem("flyway-mongodb");
    }

    @BuildStep
    InitTaskBuildItem configureInitTask(ApplicationInfoBuildItem app) {
        return InitTaskBuildItem.create()
                .withName(app.getName() + "-flyway-mongodb-init")
                .withTaskEnvVars(Map.of(
                        "QUARKUS_INIT_AND_EXIT", "true",
                        "QUARKUS_FLYWAY_MONGODB_ACTIVE", "true"))
                .withAppEnvVars(Map.of("QUARKUS_FLYWAY_MONGODB_ACTIVE", "false"))
                .withSharedEnvironment(true)
                .withSharedFilesystem(true);
    }

    public static final class MigrationStateBuildItem extends SimpleBuildItem {

        final Map<String, MigrationState> state;

        MigrationStateBuildItem(Map<String, MigrationState> state) {
            this.state = Collections.unmodifiableMap(state);
        }

        public record MigrationState(Set<String> resourcesLocations, Set<String> migrations) {
        }
    }
}
