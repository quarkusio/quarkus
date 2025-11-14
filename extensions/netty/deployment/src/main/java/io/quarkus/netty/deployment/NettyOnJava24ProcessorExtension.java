package io.quarkus.netty.deployment;

import io.netty.util.Version;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ModuleOpenBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSystemPropertyBuildItem;

/**
 * As Java 24 locks down access to sun.misc.Unsafe, Netty needs to adapt to this
 * to maintain its efficiency.
 * As this work progresses in upstream Netty to handle this better automatically,
 * we can already apply the following recommendations by the Netty team.
 * See also <a href="https://github.com/quarkusio/quarkus/issues/39907">#39907</a>
 * and <a href="https://netty.io/wiki/java-24-and-sun.misc.unsafe.html">Java 24 and sun.misc.unsafe</a>
 * </p>
 * Unfortunately, "--sun-misc-unsafe-memory-access=allow" should also be set,
 * but this can't be applied automatically as the Manifest format doesn't allow
 * setting such an option.
 */
public class NettyOnJava24ProcessorExtension {

    @BuildStep
    public NativeImageSystemPropertyBuildItem jdk24CleanersViaSetAccessible() {
        return new NativeImageSystemPropertyBuildItem("io.netty.tryReflectionSetAccessible", "true");
    }

    @BuildStep
    ModuleOpenBuildItem openModules() {
        return new ModuleOpenBuildItem("java.base", Version.class.getModule(), "java.nio", "jdk.internal.misc");
    }

}
