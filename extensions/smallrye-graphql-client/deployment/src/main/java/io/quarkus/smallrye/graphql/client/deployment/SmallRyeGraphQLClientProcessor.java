package io.quarkus.smallrye.graphql.client.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AutoInjectAnnotationBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.smallrye.graphql.client.runtime.GraphQLClientsConfig;
import io.quarkus.smallrye.graphql.client.runtime.SmallRyeGraphQLClientRecorder;

public class SmallRyeGraphQLClientProcessor {

    private static final DotName GRAPHQL_CLIENT_API = DotName
            .createSimple("io.smallrye.graphql.client.typesafe.api.GraphQLClientApi");
    private static final DotName GRAPHQL_CLIENT = DotName.createSimple("io.smallrye.graphql.client.GraphQLClient");
    private static final String NAMED_DYNAMIC_CLIENTS = "io.smallrye.graphql.client.dynamic.cdi.NamedDynamicClients";

    @BuildStep
    void feature(BuildProducer<FeatureBuildItem> featureProducer) {
        featureProducer.produce(new FeatureBuildItem(Feature.SMALLRYE_GRAPHQL_CLIENT));
    }

    @BuildStep
    void setupServiceProviders(BuildProducer<ServiceProviderBuildItem> services) {
        services.produce(ServiceProviderBuildItem
                .allProvidersFromClassPath("io.smallrye.graphql.client.typesafe.api.TypesafeGraphQLClientBuilder"));
        services.produce(ServiceProviderBuildItem
                .allProvidersFromClassPath("io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClientBuilder"));
        services.produce(ServiceProviderBuildItem.allProvidersFromClassPath("io.smallrye.graphql.client.core.Argument"));
        services.produce(ServiceProviderBuildItem.allProvidersFromClassPath("io.smallrye.graphql.client.core.Document"));
        services.produce(ServiceProviderBuildItem.allProvidersFromClassPath("io.smallrye.graphql.client.core.Enum"));
        services.produce(ServiceProviderBuildItem.allProvidersFromClassPath("io.smallrye.graphql.client.core.Field"));
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
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, apiClassInfo.name().toString()));
            for (MethodInfo method : apiClassInfo.methods()) {
                reflectiveHierarchies.produce(new ReflectiveHierarchyBuildItem.Builder()
                        .type(method.returnType())
                        .build());
                for (Type parameter : method.parameters()) {
                    reflectiveHierarchies.produce(new ReflectiveHierarchyBuildItem.Builder()
                            .type(parameter)
                            .build());
                }
            }

            // an equivalent of io.smallrye.graphql.client.typesafe.impl.cdi.GraphQlClientBean that produces typesafe client instances
            SyntheticBeanBuildItem bean = SyntheticBeanBuildItem.configure(apiClassInfo.name())
                    .addType(apiClassInfo.name())
                    .scope(Singleton.class)
                    .supplier(recorder.typesafeClientSupplier(apiClass))
                    .unremovable()
                    .done();
            syntheticBeans.produce(bean);
        }
        // needed to be able to convert config values to URI (performed by the GraphQL client code)
        reflectiveClass.produce(ReflectiveClassBuildItem.builder("java.net.URI").methods(true).build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder("java.util.List").methods(true).build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder("java.util.Collection").methods(true).build());
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    void translateClientConfiguration(BeanArchiveIndexBuildItem index,
            SmallRyeGraphQLClientRecorder recorder,
            GraphQLClientsConfig config) {
        // Map with all classes annotated with @GraphQLApi, the key is its short name,
        // value is the fully qualified name. The reason is being able to match short name
        // used in the configuration to a class
        Map<String, String> shortNamesToQualifiedNames = new HashMap<>();
        for (AnnotationInstance annotation : index.getIndex().getAnnotations(GRAPHQL_CLIENT_API)) {
            ClassInfo clazz = annotation.target().asClass();
            shortNamesToQualifiedNames.put(clazz.name().withoutPackagePrefix(), clazz.name().toString());
        }

        recorder.translateClientConfiguration(config, shortNamesToQualifiedNames);
    }

}
