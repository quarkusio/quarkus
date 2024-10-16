/*
 *    Copyright (c) 2020-2023, Aayush Atharva
 *
 *    Brotli4j licenses this file to you under the
 *    Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package io.quarkus.vertx.http.runtime.graal;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeClassInitialization;
import org.graalvm.nativeimage.hosted.RuntimeResourceAccess;

import io.quarkus.runtime.graal.GraalVM;

public class Brotli4jFeature implements Feature {

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        // We reuse Brotli4jLoader's code and logic instead of reinventing the wheel
        String customPath = System.getProperty("brotli4j.library.path");
        if (customPath != null && !customPath.isEmpty()) {
            RuntimeResourceAccess.addResource(Brotli4jFeature.class.getModule(), customPath);
        } else {
            String nativeLibName = System.mapLibraryName("brotli");
            String platform = getPlatform();
            String libPath = "lib/" + platform + '/' + nativeLibName;
            RuntimeResourceAccess.addResource(Brotli4jFeature.class.getModule(), libPath);
        }

        // We do have Brotli4J on classpath thanks to Vert.X -> Netty dependencies.
        RuntimeResourceAccess.addResource(Brotli4jFeature.class.getModule(),
                "META-INF/services/com.aayushatharva.brotli4j.service.BrotliNativeProvider");

        // Static initializer tries to load the native library in Brotli4jLoader; must be done at runtime.
        RuntimeClassInitialization.initializeAtRunTime("com.aayushatharva.brotli4j.Brotli4jLoader");
        final GraalVM.Version v = GraalVM.Version.getCurrent();
        // Newer 23.1+ GraalVM/Mandrel does not need this explicitly marked for runtime init thanks
        // to a different strategy: https://github.com/oracle/graal/blob/vm-23.1.0/substratevm/CHANGELOG.md?plain=1#L10
        if (v.compareTo(GraalVM.Version.VERSION_23_1_0) <= 0) {
            RuntimeClassInitialization.initializeAtRunTime("io.netty.handler.codec.compression.Brotli");
        }
    }

    // Verbatim copy from https://github.com/hyperxpro/Brotli4j/blob/32e3d3827fa3124ca945b75ae2138492c9c775b3/brotli4j/src/main/java/com/aayushatharva/brotli4j/Brotli4jLoader.java#L118C1-L150C6
    private static String getPlatform() {
        String osName = System.getProperty("os.name");
        String archName = System.getProperty("os.arch");

        if ("Linux".equalsIgnoreCase(osName)) {
            if ("amd64".equalsIgnoreCase(archName)) {
                return "linux-x86_64";
            } else if ("aarch64".equalsIgnoreCase(archName)) {
                return "linux-aarch64";
            } else if ("arm".equalsIgnoreCase(archName)) {
                return "linux-armv7";
            } else if ("s390x".equalsIgnoreCase(archName)) {
                return "linux-s390x";
            } else if ("ppc64le".equalsIgnoreCase(archName)) {
                return "linux-ppc64le";
            } else if ("riscv64".equalsIgnoreCase(archName)) {
                return "linux-riscv64";
            }
        } else if (osName.startsWith("Windows")) {
            if ("amd64".equalsIgnoreCase(archName)) {
                return "windows-x86_64";
            } else if ("aarch64".equalsIgnoreCase(archName)) {
                return "windows-aarch64";
            }
        } else if (osName.startsWith("Mac")) {
            if ("x86_64".equalsIgnoreCase(archName)) {
                return "osx-x86_64";
            } else if ("aarch64".equalsIgnoreCase(archName)) {
                return "osx-aarch64";
            }
        }
        throw new UnsupportedOperationException("Unsupported OS and Architecture: " + osName + ", " + archName);
    }
}
