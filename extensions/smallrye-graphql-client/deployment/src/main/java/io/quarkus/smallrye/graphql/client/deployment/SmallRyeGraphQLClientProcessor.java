package io.quarkus.smallrye.graphql.client.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.io.Closeable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Singleton;

import org.eclipse.microprofile.graphql.Input;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AutoInjectAnnotationBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeansRuntimeInitBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.smallrye.graphql.client.runtime.GraphQLClientBuildConfig;
import io.quarkus.smallrye.graphql.client.runtime.GraphQLClientCertificateUpdateEventListener;
import io.quarkus.smallrye.graphql.client.runtime.GraphQLClientSupport;
import io.quarkus.smallrye.graphql.client.runtime.SmallRyeGraphQLClientRecorder;
import io.quarkus.vertx.core.deployment.CoreVertxBuildItem;
import io.smallrye.graphql.client.model.ClientModelBuilder;
import io.smallrye.graphql.client.model.ClientModels;

public class SmallRyeGraphQLClientProcessor {

    private static final DotName GRAPHQL_CLIENT_API = DotName
            .createSimple("io.smallrye.graphql.client.typesafe.api.GraphQLClientApi");
    private static final DotName GRAPHQL_CLIENT = DotName.createSimple("io.smallrye.graphql.client.GraphQLClient");
    private static final String CERTIFICATE_UPDATE_EVENT_LISTENER = GraphQLClientCertificateUpdateEventListener.class.getName();
    private static final String NAMED_DYNAMIC_CLIENTS = "io.smallrye.graphql.client.impl.dynamic.cdi.NamedDynamicClients";

    @BuildStep
    void feature(BuildProducer<FeatureBuildItem> featureProducer) {
        featureProducer.produce(new FeatureBuildItem(Feature.SMALLRYE_GRAPHQL_CLIENT));
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(Feature.SMALLRYE_GRAPHQL_CLIENT);
    }

    @BuildStep
    void setupServiceProviders(BuildProducer<ServiceProviderBuildItem> services) {
        services.produce(ServiceProviderBuildItem
                .allProvidersFromClassPath("io.smallrye.graphql.client.typesafe.api.TypesafeGraphQLClientBuilder"));
        services.produce(ServiceProviderBuildItem
                .allProvidersFromClassPath("io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClientBuilder"));
        services.produce(
                ServiceProviderBuildItem.allProvidersFromClassPath("io.smallrye.graphql.client.core.factory.ArgumentFactory"));
        services.produce(
                ServiceProviderBuildItem.allProvidersFromClassPath("io.smallrye.graphql.client.core.factory.DirectiveFactory"));
        services.produce(
                ServiceProviderBuildItem
                        .allProvidersFromClassPath("io.smallrye.graphql.client.core.factory.DirectiveArgumentFactory"));
        services.produce(
                ServiceProviderBuildItem.allProvidersFromClassPath("io.smallrye.graphql.client.core.factory.DocumentFactory"));
        services.produce(
                ServiceProviderBuildItem.allProvidersFromClassPath("io.smallrye.graphql.client.core.factory.EnumFactory"));
        services.produce(
                ServiceProviderBuildItem.allProvidersFromClassPath("io.smallrye.graphql.client.core.factory.FieldFactory"));
        services.produce(
                ServiceProviderBuildItem.allProvidersFromClassPath("io.smallrye.graphql.client.core.factory.FragmentFactory"));
        services.produce(
                ServiceProviderBuildItem
                        .allProvidersFromClassPath("io.smallrye.graphql.client.core.factory.FragmentReferenceFactory"));
        services.produce(ServiceProviderBuildItem
                .allProvidersFromClassPath("io.smallrye.graphql.client.core.factory.InlineFragmentFactory"));
        services.produce(ServiceProviderBuildItem
                .allProvidersFromClassPath("io.smallrye.graphql.client.core.factory.InputObjectFactory"));
        services.produce(
                ServiceProviderBuildItem
                        .allProvidersFromClassPath("io.smallrye.graphql.client.core.factory.InputObjectFieldFactory"));
        services.produce(
                ServiceProviderBuildItem.allProvidersFromClassPath("io.smallrye.graphql.client.core.factory.OperationFactory"));
        services.produce(
                ServiceProviderBuildItem.allProvidersFromClassPath("io.smallrye.graphql.client.core.factory.VariableFactory"));
        services.produce(ServiceProviderBuildItem
                .allProvidersFromClassPath("io.smallrye.graphql.client.core.factory.VariableTypeFactory"));
    }

    @BuildStep
    void dynamicClientInjection(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<AutoInjectAnnotationBuildItem> autoInject) {
        additionalBeans.produce(new AdditionalBeanBuildItem(NAMED_DYNAMIC_CLIENTS));
        autoInject.produce(new AutoInjectAnnotationBuildItem(GRAPHQL_CLIENT));
    }

    @BuildStep
    @Record(STATIC_INIT)
    void initializeTypesafeClient(CombinedIndexBuildItem index,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            SmallRyeGraphQLClientRecorder recorder,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchies,
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxies) throws ClassNotFoundException {
        for (AnnotationInstance annotation : index.getIndex().getAnnotations(GRAPHQL_CLIENT_API)) {
            ClassInfo apiClassInfo = annotation.target().asClass();
            Class<?> apiClass = Class.forName(apiClassInfo.name().toString(), true,
                    Thread.currentThread().getContextClassLoader());
            proxies.produce(new NativeImageProxyDefinitionBuildItem(apiClass.getName()));

            // register the api class and all classes that it references for reflection
            reflectiveClass.produce(
                    ReflectiveClassBuildItem.builder(apiClassInfo.name().toString()).build());
            for (MethodInfo method : apiClassInfo.methods()) {
                reflectiveHierarchies.produce(ReflectiveHierarchyBuildItem
                        .builder(method.returnType())
                        .build());
                for (Type parameter : method.parameterTypes()) {
                    reflectiveHierarchies.produce(ReflectiveHierarchyBuildItem
                            .builder(parameter)
                            .build());
                }
            }

            BuiltinScope scope = BuiltinScope.from(index.getIndex().getClassByName(apiClass));
            // an equivalent of io.smallrye.graphql.client.typesafe.impl.cdi.GraphQlClientBean that produces typesafe client instances
            SyntheticBeanBuildItem bean = SyntheticBeanBuildItem.configure(apiClassInfo.name())
                    .addType(apiClassInfo.name())
                    .scope(scope == null ? BuiltinScope.APPLICATION.getInfo() : scope.getInfo())
                    .addInjectionPoint(ClassType.create(DotName.createSimple(ClientModels.class)))
                    .createWith(recorder.typesafeClientSupplier(apiClass))
                    .unremovable()
                    .done();
            syntheticBeans.produce(bean);
        }
        // needed to be able to convert config values to URI (performed by the GraphQL client code)
        reflectiveClass.produce(ReflectiveClassBuildItem.builder("java.net.URI").methods().build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder("java.util.List").methods().build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder("java.util.Collection").methods().build());
        // some more classes that the client may need to serialize
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(OffsetDateTime.class).methods().build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(Instant.class).methods().build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(ZonedDateTime.class).methods().build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(LocalDateTime.class).methods().build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(LocalTime.class).methods().build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(OffsetTime.class).methods().build());

    }

    /**
     * io.smallrye.graphql.client.GraphQLClientsConfiguration bean requires knowledge of all interfaces annotated
     * with `@GraphQLClientApi`
     */
    @BuildStep
    @Record(STATIC_INIT)
    void setTypesafeApiClasses(BeanArchiveIndexBuildItem index,
            BeanContainerBuildItem beanContainerBuildItem,
            SmallRyeGraphQLClientRecorder recorder) {
        List<String> apiClassNames = new ArrayList<>();
        for (AnnotationInstance annotation : index.getIndex().getAnnotations(GRAPHQL_CLIENT_API)) {
            ClassInfo apiClassInfo = annotation.target().asClass();
            apiClassNames.add(apiClassInfo.name().toString());
        }
        recorder.setTypesafeApiClasses(apiClassNames);
    }

    /**
     * Allows the optional usage of short class names in GraphQL client configuration rather than
     * fully qualified names. This method computes a mapping between short names and qualified names,
     * and the configuration merger bean will take it into account when merging Quarkus configuration
     * with SmallRye-side configuration.
     */
    @BuildStep
    @Record(RUNTIME_INIT)
    @Consume(SyntheticBeansRuntimeInitBuildItem.class)
    GraphQLClientConfigInitializedBuildItem mergeClientConfigurations(SmallRyeGraphQLClientRecorder recorder,
            BeanArchiveIndexBuildItem index) {
        // to store config keys of all clients found in the application code
        List<String> knownConfigKeys = new ArrayList<>();
        Map<String, String> shortNamesToQualifiedNames = new HashMap<>();
        for (AnnotationInstance annotation : index.getIndex().getAnnotations(GRAPHQL_CLIENT_API)) {
            ClassInfo clazz = annotation.target().asClass();
            shortNamesToQualifiedNames.put(clazz.name().withoutPackagePrefix(), clazz.name().toString());
            AnnotationValue configKeyValue = annotation.value("configKey");
            String configKey = configKeyValue != null ? configKeyValue.asString() : null;
            String actualConfigKey = (configKey != null && !configKey.equals("")) ? configKey : clazz.name().toString();
            knownConfigKeys.add(actualConfigKey);
        }

        for (AnnotationInstance annotation : index.getIndex().getAnnotations(GRAPHQL_CLIENT)) {
            String configKey = annotation.value().asString();
            if (configKey == null) {
                configKey = "default";
            }
            knownConfigKeys.add(configKey);
        }

        GraphQLClientSupport support = new GraphQLClientSupport();
        support.setShortNamesToQualifiedNamesMapping(shortNamesToQualifiedNames);
        support.setKnownConfigKeys(knownConfigKeys);

        recorder.mergeClientConfigurations(support);
        return new GraphQLClientConfigInitializedBuildItem();
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    void buildClientModel(CombinedIndexBuildItem index, SmallRyeGraphQLClientRecorder recorder,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans, GraphQLClientBuildConfig quarkusConfig) {
        if (!index.getIndex().getAnnotations(GRAPHQL_CLIENT_API).isEmpty()) {
            ClientModels clientModels = (quarkusConfig.enableBuildTimeScanning()) ? ClientModelBuilder.build(index.getIndex())
                    : new ClientModels(); // empty Client Model(s)
            RuntimeValue<ClientModels> modelRuntimeClientModel = recorder.getRuntimeClientModel(clientModels);
            DotName supportClassName = DotName.createSimple(ClientModels.class.getName());
            SyntheticBeanBuildItem bean = SyntheticBeanBuildItem
                    .configure(supportClassName)
                    .addType(supportClassName)
                    .scope(Singleton.class)
                    .runtimeValue(modelRuntimeClientModel)
                    .setRuntimeInit()
                    .unremovable()
                    .done();
            syntheticBeans.produce(bean);
        }
    }

    @BuildStep
    ServiceProviderBuildItem overrideErrorMessageProvider() {
        return ServiceProviderBuildItem.allProvidersFromClassPath("io.smallrye.graphql.client.impl.ErrorMessageProvider");
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    void setGlobalVertxInstance(CoreVertxBuildItem vertxBuildItem,
            SmallRyeGraphQLClientRecorder recorder) {
        recorder.setGlobalVertxInstance(vertxBuildItem.getVertx());
    }

    @BuildStep
    void setAdditionalClassesToIndex(BuildProducer<AdditionalIndexedClassesBuildItem> additionalClassesToIndex,
            GraphQLClientBuildConfig quarkusConfig) {
        if (quarkusConfig.enableBuildTimeScanning()) {
            additionalClassesToIndex.produce(new AdditionalIndexedClassesBuildItem(Closeable.class.getName()));
            additionalClassesToIndex.produce(new AdditionalIndexedClassesBuildItem(AutoCloseable.class.getName()));
            additionalClassesToIndex.produce(new AdditionalIndexedClassesBuildItem(Input.class.getName()));
        }
    }

    @BuildStep
    void registerCertificateUpdateEventListener(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(new AdditionalBeanBuildItem(CERTIFICATE_UPDATE_EVENT_LISTENER));
    }

}
