package io.quarkus.jgit.runtime.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBundleBuildItem;

class JGitProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.JGIT);
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(FeatureBuildItem.JGIT);
    }

    @BuildStep
    ReflectiveClassBuildItem reflection() {
        //Classes that use reflection
        return new ReflectiveClassBuildItem(true, true,
                "org.eclipse.jgit.internal.JGitText",
                "org.eclipse.jgit.lib.CoreConfig$AutoCRLF",
                "org.eclipse.jgit.lib.CoreConfig$CheckStat",
                "org.eclipse.jgit.lib.CoreConfig$EOL",
                "org.eclipse.jgit.lib.CoreConfig$EolStreamType",
                "org.eclipse.jgit.lib.CoreConfig$HideDotFiles",
                "org.eclipse.jgit.lib.CoreConfig$SymLinks");
    }

    @BuildStep
    RuntimeInitializedClassBuildItem lazyDigest() {
        return new RuntimeInitializedClassBuildItem("org.eclipse.jgit.transport.HttpAuthMethod$Digest");
    }

    @BuildStep
    SubstrateResourceBundleBuildItem includeResourceBundle() {
        return new SubstrateResourceBundleBuildItem("org.eclipse.jgit.internal.JGitText");
    }
}