package io.quarkus.flyway.runtime;

import java.net.URL;
import java.util.Set;

import org.flywaydb.core.internal.scanner.classpath.ClassPathLocationScanner;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * @author cristhiank on 2019-03-19
 **/
@TargetClass(className = "org.flywaydb.core.internal.scanner.classpath.JarFileClassPathLocationScanner")
public final class JarFileClassPathLocationScannerSubstitutions implements ClassPathLocationScanner {

    @Alias
    public JarFileClassPathLocationScannerSubstitutions(String separator) {
    }

    @Alias
    @Override
    public Set<String> findResourceNames(String location, URL locationUrl) {
        return null;
    }
}
