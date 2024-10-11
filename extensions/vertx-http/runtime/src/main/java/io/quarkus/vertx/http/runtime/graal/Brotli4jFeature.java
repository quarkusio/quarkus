package io.quarkus.vertx.http.runtime.graal;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeClassInitialization;
import org.graalvm.nativeimage.hosted.RuntimeResourceAccess;

import io.quarkus.runtime.graal.GraalVM;
import io.quarkus.utilities.OS;

public class Brotli4jFeature implements Feature {

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        // TODO reuse Brotli4jLoader's code instead of reinventing the wheel?
        final String arch = System.getProperty("os.arch");
        final boolean amd64 = arch.matches("^(amd64|x64|x86_64)$");
        final boolean aarch64 = "aarch64".equals(arch);
        final String lib;
        if (OS.determineOS() == OS.LINUX) {
            if (amd64) {
                lib = "linux-x86_64/libbrotli.so";
            } else if (aarch64) {
                lib = "linux-aarch64/libbrotli.so";
            } else {
                throw new IllegalStateException("Brotli compressor: No library for linux-" + arch);
            }
        } else if (OS.determineOS() == OS.WINDOWS) {
            if (amd64) {
                lib = "windows-x86_64/brotli.dll";
            } else if (aarch64) {
                lib = "windows-aarch64/brotli.dll";
            } else {
                throw new IllegalStateException("Brotli compressor: No library for windows-" + arch);
            }
        } else if (OS.determineOS() == OS.MAC) {
            if (amd64) {
                lib = "osx-x86_64/libbrotli.dylib";
            } else if (aarch64) {
                lib = "osx-aarch64/libbrotli.dylib";
            } else {
                throw new IllegalStateException("Brotli compressor: No library for osx-" + arch);
            }
        } else {
            throw new IllegalStateException("Brotli compressor: Your platform is not supported.");
        }

        // We do have Brotli4J on classpath thanks to Vert.X -> Netty dependencies.
        RuntimeResourceAccess.addResource(Brotli4jFeature.class.getModule(),
                "META-INF/services/com.aayushatharva.brotli4j.service.BrotliNativeProvider");
        // Register the Native library. We pick only the one relevant to our system.
        RuntimeResourceAccess.addResource(Brotli4jFeature.class.getModule(), "lib/" + lib);

        // Static initializer tries to load the native library in Brotli4jLoader; must be done at runtime.
        RuntimeClassInitialization.initializeAtRunTime("com.aayushatharva.brotli4j.Brotli4jLoader");
        final GraalVM.Version v = GraalVM.Version.getCurrent();
        // Newer 23.1+ GraalVM/Mandrel does not need this explicitly marked for runtime init thanks
        // to a different strategy: https://github.com/oracle/graal/blob/vm-23.1.0/substratevm/CHANGELOG.md?plain=1#L10
        if (v.compareTo(GraalVM.Version.VERSION_23_1_0) <= 0) {
            RuntimeClassInitialization.initializeAtRunTime("io.netty.handler.codec.compression.Brotli");
        }
    }

}
