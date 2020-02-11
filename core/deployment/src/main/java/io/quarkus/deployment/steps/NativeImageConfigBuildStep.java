package io.quarkus.deployment.steps;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.EnableAllSecurityServicesBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.JavaLibraryPathAdditionalPathBuildItem;
import io.quarkus.deployment.builditem.JniBuildItem;
import io.quarkus.deployment.builditem.NativeImageEnableAllCharsetsBuildItem;
import io.quarkus.deployment.builditem.NativeImageEnableAllTimeZonesBuildItem;
import io.quarkus.deployment.builditem.SslNativeConfigBuildItem;
import io.quarkus.deployment.builditem.SslTrustStoreSystemPropertyBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem;
import io.quarkus.runtime.ssl.SslContextConfigurationRecorder;

//TODO: this should go away, once we decide on which one of the API's we want
class NativeImageConfigBuildStep {

    private static final Logger log = Logger.getLogger(NativeImageConfigBuildStep.class);

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void build(SslContextConfigurationRecorder sslContextConfigurationRecorder,
            List<NativeImageConfigBuildItem> nativeImageConfigBuildItems,
            SslNativeConfigBuildItem sslNativeConfig,
            List<JniBuildItem> jniBuildItems,
            List<NativeImageEnableAllCharsetsBuildItem> nativeImageEnableAllCharsetsBuildItems,
            List<NativeImageEnableAllTimeZonesBuildItem> nativeImageEnableAllTimeZonesBuildItems,
            List<ExtensionSslNativeSupportBuildItem> extensionSslNativeSupport,
            List<EnableAllSecurityServicesBuildItem> enableAllSecurityServicesBuildItems,
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxy,
            BuildProducer<NativeImageResourceBundleBuildItem> resourceBundle,
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeInit,
            BuildProducer<RuntimeReinitializedClassBuildItem> runtimeReinit,
            BuildProducer<NativeImageSystemPropertyBuildItem> nativeImage,
            BuildProducer<SystemPropertyBuildItem> systemProperty,
            BuildProducer<JavaLibraryPathAdditionalPathBuildItem> javaLibraryPathAdditionalPath,
            BuildProducer<SslTrustStoreSystemPropertyBuildItem> sslTrustStoreSystemProperty) {
        for (NativeImageConfigBuildItem nativeImageConfigBuildItem : nativeImageConfigBuildItems) {
            for (String i : nativeImageConfigBuildItem.getRuntimeInitializedClasses()) {
                runtimeInit.produce(new RuntimeInitializedClassBuildItem(i));
            }
            for (String i : nativeImageConfigBuildItem.getRuntimeReinitializedClasses()) {
                runtimeReinit.produce(new RuntimeReinitializedClassBuildItem(i));
            }
            for (Map.Entry<String, String> e : nativeImageConfigBuildItem.getNativeImageSystemProperties().entrySet()) {
                nativeImage.produce(new NativeImageSystemPropertyBuildItem(e.getKey(), e.getValue()));
            }
            for (String i : nativeImageConfigBuildItem.getResourceBundles()) {
                resourceBundle.produce(new NativeImageResourceBundleBuildItem(i));
            }
            for (List<String> i : nativeImageConfigBuildItem.getProxyDefinitions()) {
                proxy.produce(new NativeImageProxyDefinitionBuildItem(i));
            }
        }

        // For now, we enable SSL native if it hasn't been explicitly disabled
        // it's probably overly conservative but it's a first step in the right direction
        sslContextConfigurationRecorder.setSslNativeEnabled(!sslNativeConfig.isExplicitlyDisabled());

        Boolean sslNativeEnabled = isSslNativeEnabled(sslNativeConfig, extensionSslNativeSupport);
        if (sslNativeEnabled) {
            // This makes the native image dependent on the local path used to build it.
            String graalVmHome = System.getenv("GRAALVM_HOME");
            if (graalVmHome != null) {
                Path graalVmCacertsPath = Paths.get(graalVmHome, "jre", "lib", "security", "cacerts");
                // This is useful for testing but the user will have to override it.
                sslTrustStoreSystemProperty.produce(new SslTrustStoreSystemPropertyBuildItem(graalVmCacertsPath.toString()));
            }
        }
        nativeImage.produce(new NativeImageSystemPropertyBuildItem("quarkus.ssl.native", sslNativeEnabled.toString()));

        if (!enableAllSecurityServicesBuildItems.isEmpty()) {
            nativeImage.produce(new NativeImageSystemPropertyBuildItem("quarkus.native.enable-all-security-services", "true"));
        }

        for (JniBuildItem jniBuildItem : jniBuildItems) {
            if (jniBuildItem.getLibraryPaths() != null && !jniBuildItem.getLibraryPaths().isEmpty()) {
                for (String path : jniBuildItem.getLibraryPaths()) {
                    javaLibraryPathAdditionalPath.produce(new JavaLibraryPathAdditionalPathBuildItem(path));
                }
            }
        }

        if (!nativeImageEnableAllCharsetsBuildItems.isEmpty()) {
            nativeImage.produce(new NativeImageSystemPropertyBuildItem("quarkus.native.enable-all-charsets", "true"));
        }

        if (!nativeImageEnableAllTimeZonesBuildItems.isEmpty()) {
            nativeImage.produce(new NativeImageSystemPropertyBuildItem("quarkus.native.enable-all-timezones", "true"));
        }
    }

    private Boolean isSslNativeEnabled(SslNativeConfigBuildItem sslNativeConfig,
            List<ExtensionSslNativeSupportBuildItem> extensionSslNativeSupport) {
        if (sslNativeConfig.isEnabled()) {
            return Boolean.TRUE;
        } else if (!sslNativeConfig.isExplicitlyDisabled() && !extensionSslNativeSupport.isEmpty()) {
            // we have extensions desiring the SSL support and it's not explicitly disabled
            if (log.isDebugEnabled()) {
                log.debugf("Native SSL support enabled due to extensions [%s] requiring it",
                        extensionSslNativeSupport.stream().map(s -> s.getExtension()).collect(Collectors.joining(", ")));
            }
            return Boolean.TRUE;
        }

        return Boolean.FALSE;
    }
}
