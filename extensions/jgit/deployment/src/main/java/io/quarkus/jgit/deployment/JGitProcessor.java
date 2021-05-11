package io.quarkus.jgit.deployment;

import java.util.Arrays;
import java.util.List;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

class JGitProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.JGIT);
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(Feature.JGIT);
    }

    @BuildStep
    ReflectiveClassBuildItem reflection() {
        //Classes that use reflection
        return new ReflectiveClassBuildItem(true, true,
                "org.eclipse.jgit.api.MergeCommand$FastForwardMode",
                "org.eclipse.jgit.api.MergeCommand$FastForwardMode$Merge",
                "org.eclipse.jgit.internal.JGitText",
                "org.eclipse.jgit.lib.CoreConfig$AutoCRLF",
                "org.eclipse.jgit.lib.CoreConfig$CheckStat",
                "org.eclipse.jgit.lib.CoreConfig$EOL",
                "org.eclipse.jgit.lib.CoreConfig$EolStreamType",
                "org.eclipse.jgit.lib.CoreConfig$HideDotFiles",
                "org.eclipse.jgit.lib.CoreConfig$SymLinks",
                "org.eclipse.jgit.lib.CoreConfig$LogRefUpdates");
    }

    @BuildStep
    List<RuntimeInitializedClassBuildItem> runtimeInitializedClasses() {
        return Arrays.asList(
                new RuntimeInitializedClassBuildItem("org.eclipse.jgit.transport.HttpAuthMethod$Digest"),
                new RuntimeInitializedClassBuildItem("org.eclipse.jgit.lib.GpgSigner"),
                // The following classes use j.u.Ramdom, so they need to be runtime-initialized
                new RuntimeInitializedClassBuildItem("org.eclipse.jgit.internal.storage.file.WindowCache"),
                new RuntimeInitializedClassBuildItem("org.eclipse.jgit.util.FileUtils"));
    }

    @BuildStep
    NativeImageResourceBundleBuildItem includeResourceBundle() {
        return new NativeImageResourceBundleBuildItem("org.eclipse.jgit.internal.JGitText");
    }
}
