package io.quarkus.flyway.runtime.graal;

import java.nio.charset.Charset;
import java.util.Collection;

import org.flywaydb.core.api.Location;
import org.flywaydb.core.internal.scanner.LocationScannerCache;
import org.flywaydb.core.internal.scanner.ResourceNameCache;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Needed to get rid of some Android related classes
 */
@TargetClass(className = "org.flywaydb.core.internal.scanner.Scanner")
public final class ScannerSubstitutions {

    @Substitute
    public ScannerSubstitutions(Class<?> implementedInterface, Collection<Location> locations, ClassLoader classLoader,
            Charset encoding,
            boolean detectEncoding,
            boolean stream,
            ResourceNameCache resourceNameCache, LocationScannerCache locationScannerCache,
            boolean throwOnMissingLocations) {
        throw new IllegalStateException("'org.flywaydb.core.internal.scanner.Scanner' is never used in Quarkus");
    }
}
