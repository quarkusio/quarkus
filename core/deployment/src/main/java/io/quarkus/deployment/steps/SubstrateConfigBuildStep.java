/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.deployment.steps;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.graalvm.nativeimage.ImageInfo;
import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.JavaLibraryPathAdditionalPathBuildItem;
import io.quarkus.deployment.builditem.SslNativeConfigBuildItem;
import io.quarkus.deployment.builditem.SslTrustStoreSystemPropertyBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.substrate.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.substrate.RuntimeReinitializedClassBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateConfigBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBundleBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateSystemPropertyBuildItem;

//TODO: this should go away, once we decide on which one of the API's we want
class SubstrateConfigBuildStep {

    private static final Logger log = Logger.getLogger(SubstrateConfigBuildStep.class);

    @BuildStep
    void build(List<SubstrateConfigBuildItem> substrateConfigBuildItems,
            SslNativeConfigBuildItem sslNativeConfig,
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

        if (sslNativeEnabled) {
            // This is an ugly hack but for now it's the only way to make the SunEC library
            // available to the native image.
            // This makes the native image dependent on the local path used to build it.
            // If you want to push your native image to a different environment, you will
            // need to put libsunec.so aside the native image or override java.library.path.

            String graalVmHome = System.getenv("GRAALVM_HOME");

            if (graalVmHome != null) {
                Path graalVmLibDirectory = Paths.get(graalVmHome, "jre", "lib");
                Path amd64LibDirectory = graalVmLibDirectory.resolve("amd64");

                // We add . as it might be useful in a containerized world
                // FIXME: it seems GraalVM does not support having multiple paths in java.library.path
                //javaLibraryPathAdditionalPath.produce(new JavaLibraryPathAdditionalPathBuildItem("."));
                if (existsAndDoesNotContainOnlyTheAgent(amd64LibDirectory)) {
                    // On Linux, the SunEC library is in jre/lib/amd64/
                    // This is useful for testing or if you have a similar environment in production
                    javaLibraryPathAdditionalPath
                            .produce(new JavaLibraryPathAdditionalPathBuildItem(amd64LibDirectory.toString()));
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
    }

    /**
     * In GrallVM 1.0.0-rc14 a new amd64 directory has been added on Mac OS X. This directory was already existing on
     * Linux, but not on Mac OS X. This directory contains the {@code libnative-image-agent.dylib}. So the check
     * on the existence of the directory is not enough anymore. So, we need to check that the directory exist AND
     * contains more than 1 file (the agent).
     *
     * @param path the {@code amd64} path
     * @return whether or not the {@code amd64} path exists AND contains more than 1 file.
     */
    private boolean existsAndDoesNotContainOnlyTheAgent(Path path) {
        if (!Files.exists(path)) {
            return false;
        }

        try (Stream<Path> children = Files.list(path)) {
            return children
                    .map(Path::getFileName)
                    .filter(fileName -> !fileName.toFile().getName().startsWith(".")) // Skip hidden files (.DS_Store)
                    .count() > 1;
            // If we get more than 1, it means that we have other files in this directory.
            // So we should use that directory for `java.library.path`.
        } catch (IOException e) {
            return false;
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
