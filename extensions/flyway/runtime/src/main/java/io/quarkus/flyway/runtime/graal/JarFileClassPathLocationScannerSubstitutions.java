package io.quarkus.flyway.runtime.graal;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Just make the constructor visible to ClassPathScannerSubstitutions.
 */
@TargetClass(className = "org.flywaydb.core.internal.scanner.classpath.JarFileClassPathLocationScanner")
public final class JarFileClassPathLocationScannerSubstitutions {

    @Alias
    public JarFileClassPathLocationScannerSubstitutions(String separator) {
    }
}