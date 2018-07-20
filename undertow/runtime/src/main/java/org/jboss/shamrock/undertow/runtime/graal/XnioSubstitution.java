package org.jboss.shamrock.undertow.runtime.graal;

import org.xnio.Xnio;
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
}
