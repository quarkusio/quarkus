package io.quarkus.undertow.common.runtime.graal;

import java.io.Closeable;

import org.xnio.IoUtils;
import org.xnio.Xnio;
import org.xnio.management.XnioProviderMXBean;
import org.xnio.management.XnioServerMXBean;
import org.xnio.management.XnioWorkerMXBean;
import org.xnio.nio.NioXnioProvider;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.xnio.Xnio")
final class XnioSubstitution {

    @Substitute
    public static Xnio getInstance() {
        return new NioXnioProvider().getInstance();
    }

    @Substitute
    public static Xnio getInstance(final ClassLoader classLoader) {
        return new NioXnioProvider().getInstance();
    }

    @Substitute
    protected static Closeable register(XnioProviderMXBean providerMXBean) {
        return IoUtils.nullCloseable();
    }

    @Substitute
    protected static Closeable register(XnioWorkerMXBean workerMXBean) {
        return IoUtils.nullCloseable();
    }

    @Substitute
    protected static Closeable register(XnioServerMXBean serverMXBean) {
        return IoUtils.nullCloseable();
    }
}
