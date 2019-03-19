package io.quarkus.flyway.runtime;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.flywaydb.core.internal.scanner.classpath.ClassPathLocationScanner;
import org.flywaydb.core.internal.scanner.classpath.FileSystemClassPathLocationScanner;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * @author cristhiank on 2019-03-19
 **/
@TargetClass(className = "org.flywaydb.core.internal.scanner.classpath.ClassPathScanner")
public final class ClassPathScannerSubstitutions {
    @Alias
    private Map<String, ClassPathLocationScanner> locationScannerCache = new HashMap<>();
    @Alias
    private Map<ClassPathLocationScanner, Map<URL, Set<String>>> resourceNameCache = new HashMap<>();

    @Substitute
    private ClassPathLocationScanner createLocationScanner(String protocol) {
        if (locationScannerCache.containsKey(protocol)) {
            return locationScannerCache.get(protocol);
        }

        if ("file".equals(protocol)) {
            FileSystemClassPathLocationScanner locationScanner = new FileSystemClassPathLocationScanner();
            locationScannerCache.put(protocol, locationScanner);
            resourceNameCache.put(locationScanner, new HashMap<>());
            return locationScanner;
        }

        if ("jar".equals(protocol)) {
            String separator = "!/";
            ClassPathLocationScanner locationScanner = new JarFileClassPathLocationScannerSubstitutions(separator);
            locationScannerCache.put(protocol, locationScanner);
            resourceNameCache.put(locationScanner, new HashMap<>());
            return locationScanner;
        }

        return null;
    }
}
