package io.quarkus.flyway.runtime.graal;

import java.util.HashMap;
import java.util.function.BooleanSupplier;

import org.flywaydb.core.internal.scanner.LocationScannerCache;
import org.flywaydb.core.internal.scanner.ResourceNameCache;
import org.flywaydb.core.internal.scanner.classpath.ClassPathLocationScanner;
import org.flywaydb.core.internal.scanner.classpath.FileSystemClassPathLocationScanner;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Get rid of JBoss VFS if it is not present in the classpath.
 */
@TargetClass(className = "org.flywaydb.core.internal.scanner.classpath.ClassPathScanner", onlyWith = ClassPathScannerSubstitutions.IsJBossVFSAbsent.class)
public final class ClassPathScannerSubstitutions {

    @Alias
    private LocationScannerCache locationScannerCache;

    @Alias
    private ResourceNameCache resourceNameCache;

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
            ClassPathLocationScanner locationScanner = (ClassPathLocationScanner) (Object) new JarFileClassPathLocationScannerSubstitutions(
                    separator);
            locationScannerCache.put(protocol, locationScanner);
            resourceNameCache.put(locationScanner, new HashMap<>());
            return locationScanner;
        }

        return null;
    }

    static final class IsJBossVFSAbsent implements BooleanSupplier {

        @Override
        public boolean getAsBoolean() {
            try {
                Class.forName("org.jboss.vfs.VFS");
                return false;
            } catch (ClassNotFoundException e) {
                return true;
            }
        }
    }

}
