package io.quarkus.hazelcast.client.deployment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

import com.hazelcast.client.cache.impl.HazelcastClientCachingProvider;
import com.hazelcast.client.impl.ClientExtension;
import com.hazelcast.client.impl.spi.ClientProxyFactory;
import com.hazelcast.cluster.MembershipListener;
import com.hazelcast.collection.ItemListener;
import com.hazelcast.com.fasterxml.jackson.core.JsonFactory;
import com.hazelcast.config.EventJournalConfig;
import com.hazelcast.config.MerkleTreeConfig;
import com.hazelcast.config.replacer.EncryptionReplacer;
import com.hazelcast.config.replacer.PropertyReplacer;
import com.hazelcast.config.replacer.spi.ConfigReplacer;
import com.hazelcast.core.EntryListener;
import com.hazelcast.internal.config.DomConfigHelper;
import com.hazelcast.internal.util.ICMPHelper;
import com.hazelcast.map.listener.MapListener;
import com.hazelcast.nio.SocketInterceptor;
import com.hazelcast.nio.serialization.DataSerializable;
import com.hazelcast.nio.serialization.DataSerializableFactory;
import com.hazelcast.nio.serialization.PortableFactory;
import com.hazelcast.nio.serialization.Serializer;
import com.hazelcast.nio.ssl.BasicSSLContextFactory;
import com.hazelcast.partition.MigrationListener;
import com.hazelcast.spi.discovery.DiscoveryStrategy;
import com.hazelcast.spi.discovery.DiscoveryStrategyFactory;
import com.hazelcast.spi.discovery.NodeFilter;
import com.hazelcast.spi.discovery.multicast.MulticastDiscoveryStrategy;
import com.hazelcast.topic.MessageListener;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyIgnoreWarningBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.hazelcast.client.runtime.HazelcastClientBytecodeRecorder;
import io.quarkus.hazelcast.client.runtime.HazelcastClientConfig;
import io.quarkus.hazelcast.client.runtime.HazelcastClientProducer;

/**
 * @author Grzegorz Piwowarek
 */
class HazelcastClientProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.HAZELCAST_CLIENT);
    }

    @BuildStep
    void enableSSL(BuildProducer<ExtensionSslNativeSupportBuildItem> ssl) {
        ssl.produce(new ExtensionSslNativeSupportBuildItem(FeatureBuildItem.HAZELCAST_CLIENT));
    }

    @BuildStep
    void registerServiceProviders(BuildProducer<ServiceProviderBuildItem> services) throws IOException {
        registerServiceProviders(DiscoveryStrategyFactory.class, services);
        registerServiceProviders(ClientExtension.class, services);
        registerServiceProviders(JsonFactory.class, services);
    }

    @BuildStep
    void setup(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(HazelcastClientProducer.class));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    HazelcastClientConfiguredBuildItem resolveClientProperties(HazelcastClientBytecodeRecorder recorder,
            HazelcastClientConfig config) {
        recorder.configureRuntimeProperties(config);
        return new HazelcastClientConfiguredBuildItem();
    }

    @BuildStep
    void registerConfigurationFiles(
            BuildProducer<NativeImageResourceBuildItem> resources,
            BuildProducer<HotDeploymentWatchedFileBuildItem> watchedFiles) {
        resources.produce(new NativeImageResourceBuildItem(
                "hazelcast-client.yml",
                "hazelcast-client-default.xml",
                "hazelcast-client.yaml",
                "hazelcast-client.xml"));

        watchedFiles.produce(new HotDeploymentWatchedFileBuildItem("hazelcast-client.yml"));
        watchedFiles.produce(new HotDeploymentWatchedFileBuildItem("hazelcast-client.yaml"));
        watchedFiles.produce(new HotDeploymentWatchedFileBuildItem("hazelcast-client.xml"));
    }

    @BuildStep
    void registerReflectivelyCreatedClasses(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        reflectiveClasses.produce(new ReflectiveClassBuildItem(false, false,
                HazelcastClientCachingProvider.class,
                DomConfigHelper.class,
                EventJournalConfig.class,
                MerkleTreeConfig.class));
    }

    @BuildStep
    void registerCustomImplementationClasses(BuildProducer<ReflectiveHierarchyBuildItem> reflectiveClassHierarchies,
            BuildProducer<ReflectiveHierarchyIgnoreWarningBuildItem> ignoreWarnings) {

        registerTypeHierarchy(reflectiveClassHierarchies, ignoreWarnings,
                SocketInterceptor.class,
                MembershipListener.class,
                MigrationListener.class,
                EntryListener.class,
                MessageListener.class,
                ItemListener.class,
                MapListener.class,
                com.hazelcast.client.impl.ClientExtension.class,
                ClientProxyFactory.class);
    }

    @BuildStep
    void registerCustomConfigReplacerClasses(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveClassHierarchies,
            BuildProducer<ReflectiveHierarchyIgnoreWarningBuildItem> ignoreWarnings) {

        registerTypeHierarchy(reflectiveClassHierarchies, ignoreWarnings, ConfigReplacer.class);
        reflectiveClasses.produce(new ReflectiveClassBuildItem(false, false,
                EncryptionReplacer.class,
                PropertyReplacer.class));
    }

    @BuildStep
    void registerCustomDiscoveryStrategiesClasses(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveClassHierarchies,
            BuildProducer<ReflectiveHierarchyIgnoreWarningBuildItem> ignoreWarnings) {

        registerTypeHierarchy(reflectiveClassHierarchies, ignoreWarnings,
                DiscoveryStrategy.class,
                NodeFilter.class);

        reflectiveClasses.produce(new ReflectiveClassBuildItem(false, false, MulticastDiscoveryStrategy.class));
        reflectiveClasses.produce(new ReflectiveClassBuildItem(false, false,
                "com.hazelcast.aws.AwsDiscoveryStrategy",
                "com.hazelcast.aws.AwsDiscoveryStrategyFactory",
                "com.hazelcast.gcp.GcpDiscoveryStrategy",
                "com.hazelcast.gcp.GcpDiscoveryStrategyFactory"));

        reflectiveClasses.produce(new ReflectiveClassBuildItem(false, false,
                "com.hazelcast.kubernetes.HazelcastKubernetesDiscoveryStrategyFactory",
                "com.hazelcast.kubernetes.HazelcastKubernetesDiscoveryStrategy"));
    }

    void registerServiceProviders(Class<?> klass, BuildProducer<ServiceProviderBuildItem> services) throws IOException {
        String service = "META-INF/services/" + klass.getName();

        Set<String> implementations = ServiceUtil
                .classNamesNamedIn(Thread.currentThread().getContextClassLoader(), service);

        services.produce(new ServiceProviderBuildItem(klass.getName(), new ArrayList<>(implementations)));
    }

    @BuildStep
    void registerCustomCredentialFactories(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveClassHierarchies,
            BuildProducer<ReflectiveHierarchyIgnoreWarningBuildItem> ignoreWarnings) {
        registerTypeHierarchy(
                reflectiveClassHierarchies, ignoreWarnings,
                com.hazelcast.security.ICredentialsFactory.class);

        reflectiveClasses.produce(
                new ReflectiveClassBuildItem(false, false, com.hazelcast.config.security.StaticCredentialsFactory.class));
    }

    @BuildStep
    void registerSSLUtilities(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveClassHierarchies,
            BuildProducer<ReflectiveHierarchyIgnoreWarningBuildItem> ignoreWarnings) {

        registerTypeHierarchy(
                reflectiveClassHierarchies, ignoreWarnings,
                com.hazelcast.nio.ssl.SSLContextFactory.class);
        reflectiveClasses.produce(
                new ReflectiveClassBuildItem(false, false, BasicSSLContextFactory.class));
    }

    @BuildStep
    void registerUserImplementationsOfSerializableUtilities(
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveClassHierarchies,
            BuildProducer<ReflectiveHierarchyIgnoreWarningBuildItem> ignoreWarnings) {
        registerTypeHierarchy(reflectiveClassHierarchies, ignoreWarnings,
                DataSerializable.class,
                DataSerializableFactory.class,
                PortableFactory.class,
                Serializer.class);
    }

    @BuildStep
    void registerICMPHelper(BuildProducer<NativeImageResourceBuildItem> resources,
            BuildProducer<RuntimeReinitializedClassBuildItem> reinitializedClasses) {
        resources.produce(new NativeImageResourceBuildItem(
                "lib/linux-x86/libicmp_helper.so",
                "lib/linux-x86_64/libicmp_helper.so"));
        reinitializedClasses.produce(new RuntimeReinitializedClassBuildItem(ICMPHelper.class.getName()));
    }

    @BuildStep
    void registerXMLParsingUtilities(BuildProducer<NativeImageResourceBuildItem> resources) {
        resources.produce(new NativeImageResourceBuildItem("hazelcast-client-config-4.0.xsd"));
    }

    private static void registerTypeHierarchy(
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchyClass,
            BuildProducer<ReflectiveHierarchyIgnoreWarningBuildItem> ignoreWarnings,
            Class<?>... classNames) {

        for (Class<?> klass : classNames) {
            DotName simpleName = DotName.createSimple(klass.getName());

            reflectiveHierarchyClass
                    .produce(new ReflectiveHierarchyBuildItem(Type.create(simpleName, Type.Kind.CLASS)));
            ignoreWarnings.produce(new ReflectiveHierarchyIgnoreWarningBuildItem(simpleName));
        }
    }
}
