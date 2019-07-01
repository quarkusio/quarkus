package io.quarkus.deployment.steps;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.graalvm.nativeimage.ImageInfo;
import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.JavaLibraryPathAdditionalPathBuildItem;
import io.quarkus.deployment.builditem.JniBuildItem;
import io.quarkus.deployment.builditem.SslNativeConfigBuildItem;
import io.quarkus.deployment.builditem.SslTrustStoreSystemPropertyBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.substrate.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.substrate.RuntimeReinitializedClassBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateConfigBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBundleBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateSystemPropertyBuildItem;
import io.quarkus.runtime.ssl.SslContextConfigurationTemplate;

//TODO: this should go away, once we decide on which one of the API's we want
class SubstrateConfigBuildStep {

    private static final Logger log = Logger.getLogger(SubstrateConfigBuildStep.class);

    private static final String LIB_SUN_EC = "libsunec.so";

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void build(SslContextConfigurationTemplate sslContextConfigurationTemplate,
            List<SubstrateConfigBuildItem> substrateConfigBuildItems,
            SslNativeConfigBuildItem sslNativeConfig,
            List<JniBuildItem> jniBuildItems,
            List<ExtensionSslNativeSupportBuildItem> extensionSslNativeSupport,
            BuildProducer<SubstrateProxyDefinitionBuildItem> proxy,
            BuildProducer<SubstrateResourceBundleBuildItem> resourceBundle,
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeInit,
            BuildProducer<RuntimeReinitializedClassBuildItem> runtimeReinit,
            BuildProducer<SubstrateSystemPropertyBuildItem> nativeImage,
            BuildProducer<SystemPropertyBuildItem> systemProperty,
            BuildProducer<JavaLibraryPathAdditionalPathBuildItem> javaLibraryPathAdditionalPath,
            BuildProducer<SslTrustStoreSystemPropertyBuildItem> sslTrustStoreSystemProperty) {
        for (SubstrateConfigBuildItem substrateConfigBuildItem : substrateConfigBuildItems) {
            for (String i : substrateConfigBuildItem.getRuntimeInitializedClasses()) {
                runtimeInit.produce(new RuntimeInitializedClassBuildItem(i));
            }
            for (String i : substrateConfigBuildItem.getRuntimeReinitializedClasses()) {
                runtimeReinit.produce(new RuntimeReinitializedClassBuildItem(i));
            }
            for (Map.Entry<String, String> e : substrateConfigBuildItem.getNativeImageSystemProperties().entrySet()) {
                nativeImage.produce(new SubstrateSystemPropertyBuildItem(e.getKey(), e.getValue()));
            }
            for (String i : substrateConfigBuildItem.getResourceBundles()) {
                resourceBundle.produce(new SubstrateResourceBundleBuildItem(i));
            }
            for (List<String> i : substrateConfigBuildItem.getProxyDefinitions()) {
                proxy.produce(new SubstrateProxyDefinitionBuildItem(i));
            }
        }

        Boolean sslNativeEnabled = isSslNativeEnabled(sslNativeConfig, extensionSslNativeSupport);

        // For now, we enable SSL native if it hasn't been explicitly disabled
        // it's probably overly conservative but it's a first step in the right direction
        sslContextConfigurationTemplate.setSslNativeEnabled(!sslNativeConfig.isExplicitlyDisabled());

        if (sslNativeEnabled) {
            // This is an ugly hack but for now it's the only way to make the SunEC library
            // available to the native image.
            // This makes the native image dependent on the local path used to build it.
            // If you want to push your native image to a different environment, you will
            // need to put libsunec.so aside the native image or override java.library.path.

            String graalVmHome = System.getenv("GRAALVM_HOME");

            if (graalVmHome != null) {
                Path graalVmLibDirectory = Paths.get(graalVmHome, "jre", "lib");
                Path linuxLibDirectory = graalVmLibDirectory.resolve("amd64");
                Path linuxPath = linuxLibDirectory.resolve(LIB_SUN_EC);

                // We add . as it might be useful in a containerized world
                javaLibraryPathAdditionalPath.produce(new JavaLibraryPathAdditionalPathBuildItem("."));
                if (Files.exists(linuxPath)) {
                    // On Linux, the SunEC library is in jre/lib/amd64/
                    // This is useful for testing or if you have a similar environment in production
                    javaLibraryPathAdditionalPath
                            .produce(new JavaLibraryPathAdditionalPathBuildItem(linuxLibDirectory.toString()));
                } else {
                    // On MacOS, the SunEC library is directly in jre/lib/
                    // This is useful for testing or if you have a similar environment in production
                    systemProperty.produce(new SystemPropertyBuildItem("java.library.path", graalVmLibDirectory.toString()));
                }

                // This is useful for testing but the user will have to override it.
                sslTrustStoreSystemProperty.produce(
                        new SslTrustStoreSystemPropertyBuildItem(
                                graalVmLibDirectory.resolve(Paths.get("security", "cacerts")).toString()));
            } else {
                // only warn if we're building a native image
                if (ImageInfo.inImageBuildtimeCode()) {
                    log.warn(
                            "SSL is enabled but the GRAALVM_HOME environment variable is not set. The java.library.path property has not been set and will need to be set manually.");
                }
            }
        }
        nativeImage.produce(new SubstrateSystemPropertyBuildItem("quarkus.ssl.native", sslNativeEnabled.toString()));

        if (!jniBuildItems.isEmpty()) {
            for (JniBuildItem jniBuildItem : jniBuildItems) {
                if (jniBuildItem.getLibraryPaths() != null && !jniBuildItem.getLibraryPaths().isEmpty()) {
                    for (String path : jniBuildItem.getLibraryPaths()) {
                        javaLibraryPathAdditionalPath
                                .produce(new JavaLibraryPathAdditionalPathBuildItem(path));
                    }
                }
            }
            nativeImage.produce(new SubstrateSystemPropertyBuildItem("quarkus.jni.enable", "true"));
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
