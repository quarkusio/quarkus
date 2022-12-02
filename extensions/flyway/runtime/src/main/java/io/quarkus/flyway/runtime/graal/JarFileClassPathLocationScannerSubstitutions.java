package io.quarkus.flyway.runtime.graal;

import org.flywaydb.core.internal.scanner.classpath.JarFileClassPathLocationScanner;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Just make the constructor visible to ClassPathScannerSubstitutions.
 */
@TargetClass(JarFileClassPathLocationScanner.class)
public final class JarFileClassPathLocationScannerSubstitutions {

    @Alias
    public JarFileClassPathLocationScannerSubstitutions(String separator) {
    }
}
