package io.quarkus.smallrye.graphql.client.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AutoInjectAnnotationBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.smallrye.graphql.client.runtime.GraphQLClientConfigurationMergerBean;
import io.quarkus.smallrye.graphql.client.runtime.GraphQLClientSupport;
import io.quarkus.smallrye.graphql.client.runtime.GraphQLClientsConfig;
import io.quarkus.smallrye.graphql.client.runtime.SmallRyeGraphQLClientRecorder;
import io.quarkus.vertx.core.deployment.CoreVertxBuildItem;

public class SmallRyeGraphQLClientProcessor {

    private static final DotName GRAPHQL_CLIENT_API = DotName
            .createSimple("io.smallrye.graphql.client.typesafe.api.GraphQLClientApi");
    private static final DotName GRAPHQL_CLIENT = DotName.createSimple("io.smallrye.graphql.client.GraphQLClient");
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
        services.produce(ServiceProviderBuildItem.allProvidersFromClassPath("io.smallrye.graphql.client.core.Argument"));
        services.produce(ServiceProviderBuildItem.allProvidersFromClassPath("io.smallrye.graphql.client.core.Directive"));
        services.produce(
                ServiceProviderBuildItem.allProvidersFromClassPath("io.smallrye.graphql.client.core.DirectiveArgument"));
        services.produce(ServiceProviderBuildItem.allProvidersFromClassPath("io.smallrye.graphql.client.core.Document"));
        services.produce(ServiceProviderBuildItem.allProvidersFromClassPath("io.smallrye.graphql.client.core.Enum"));
        services.produce(ServiceProviderBuildItem.allProvidersFromClassPath("io.smallrye.graphql.client.core.Field"));
        services.produce(ServiceProviderBuildItem.allProvidersFromClassPath("io.smallrye.graphql.client.core.Fragment"));
        services.produce(
                ServiceProviderBuildItem.allProvidersFromClassPath("io.smallrye.graphql.client.core.FragmentReference"));
        services.produce(ServiceProviderBuildItem.allProvidersFromClassPath("io.smallrye.graphql.client.core.InlineFragment"));
        services.produce(ServiceProviderBuildItem.allProvidersFromClassPath("io.smallrye.graphql.client.core.InputObject"));
        services.produce(
                ServiceProviderBuildItem.allProvidersFromClassPath("io.smallrye.graphql.client.core.InputObjectField"));
        services.produce(ServiceProviderBuildItem.allProvidersFromClassPath("io.smallrye.graphql.client.core.Operation"));
        services.produce(ServiceProviderBuildItem.allProvidersFromClassPath("io.smallrye.graphql.client.core.Variable"));
        services.produce(ServiceProviderBuildItem.allProvidersFromClassPath("io.smallrye.graphql.client.core.VariableType"));
    }

    @BuildStep
    void dynamicClientInjection(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<AutoInjectAnnotationBuildItem> autoInject) {
        additionalBeans.produce(new AdditionalBeanBuildItem(NAMED_DYNAMIC_CLIENTS));
        autoInject.produce(new AutoInjectAnnotationBuildItem(GRAPHQL_CLIENT));
    }

    @BuildStep
    @Record(STATIC_INIT)
    void initializeTypesafeClient(BeanArchiveIndexBuildItem index,
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
                reflectiveHierarchies.produce(new ReflectiveHierarchyBuildItem.Builder()
                        .type(method.returnType())
                        .build());
                for (Type parameter : method.parameterTypes()) {
                    reflectiveHierarchies.produce(new ReflectiveHierarchyBuildItem.Builder()
                            .type(parameter)
                            .build());
                }
            }

            // an equivalent of io.smallrye.graphql.client.typesafe.impl.cdi.GraphQlClientBean that produces typesafe client instances
            SyntheticBeanBuildItem bean = SyntheticBeanBuildItem.configure(apiClassInfo.name())
                    .addType(apiClassInfo.name())
                    .scope(ApplicationScoped.class)
                    .supplier(recorder.typesafeClientSupplier(apiClass))
                    .unremovable()
                    .done();
            syntheticBeans.produce(bean);
        }
        // needed to be able to convert config values to URI (performed by the GraphQL client code)
        reflectiveClass.produce(ReflectiveClassBuildItem.builder("java.net.URI").methods().build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder("java.util.List").methods().build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder("java.util.Collection").methods().build());
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
    void initializeClientSupport(BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            SmallRyeGraphQLClientRecorder recorder,
            GraphQLClientsConfig quarkusConfig,
            BeanArchiveIndexBuildItem index) {
        // to store config keys of all clients found in the application code
        List<String> knownConfigKeys = new ArrayList<>();

        Map<String, String> shortNamesToQualifiedNames = new HashMap<>();
        for (AnnotationInstance annotation : index.getIndex().getAnnotations(GRAPHQL_CLIENT_API)) {
            ClassInfo clazz = annotation.target().asClass();
            shortNamesToQualifiedNames.put(clazz.name().withoutPackagePrefix(), clazz.name().toString());
            AnnotationValue configKeyValue = annotation.value("configKey");
            String configKey = configKeyValue != null ? configKeyValue.asString() : null;
            knownConfigKeys.add((configKey != null && !configKey.equals("")) ? configKey : clazz.name().toString());
        }

        for (AnnotationInstance annotation : index.getIndex().getAnnotations(GRAPHQL_CLIENT)) {
            String configKey = annotation.value().asString();
            if (configKey == null) {
                configKey = "default";
            }
            knownConfigKeys.add(configKey);
        }

        RuntimeValue<GraphQLClientSupport> support = recorder.clientSupport(shortNamesToQualifiedNames, knownConfigKeys);

        DotName supportClassName = DotName.createSimple(GraphQLClientSupport.class.getName());
        SyntheticBeanBuildItem bean = SyntheticBeanBuildItem
                .configure(supportClassName)
                .addType(supportClassName)
                .scope(Singleton.class)
                .runtimeValue(support)
                .setRuntimeInit()
                .unremovable()
                .done();
        syntheticBeans.produce(bean);
    }

    @BuildStep
    AdditionalBeanBuildItem configurationMergerBean() {
        return AdditionalBeanBuildItem.unremovableOf(GraphQLClientConfigurationMergerBean.class);
    }

    // FIXME: this seems unnecessary, but is needed to make sure that the GraphQLClientConfigurationMergerBean
    // gets initialized, can this be done differently?
    @BuildStep
    @Record(RUNTIME_INIT)
    void initializeConfigMergerBean(BeanContainerBuildItem containerBuildItem,
            SmallRyeGraphQLClientRecorder recorder) {
        recorder.initializeConfigurationMergerBean();
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

}
