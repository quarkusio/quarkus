package io.quarkus.jgit.runtime;

import java.io.File;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.eclipse.jgit.util.FS")
public final class FSSubstitution {

    /**
     * The original method caches the user.home property during build time.
     *
     * TODO: Find a way to call userHomeImpl() instead and cache the result
     */
    @Substitute
    public File userHome() {
        return new File(System.getProperty("user.home")).getAbsoluteFile();
    }
}
