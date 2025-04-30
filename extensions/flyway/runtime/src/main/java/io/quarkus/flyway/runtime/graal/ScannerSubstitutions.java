package io.quarkus.flyway.runtime.graal;

import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.internal.scanner.LocationScannerCache;
import org.flywaydb.core.internal.scanner.ResourceNameCache;
import org.flywaydb.core.internal.scanner.Scanner;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Needed to get rid of some Android related classes
 */
@TargetClass(Scanner.class)
public final class ScannerSubstitutions<I> {

    @Substitute
    public ScannerSubstitutions(
            Class<I> implementedInterface,
            boolean stream,
            ResourceNameCache resourceNameCache,
            LocationScannerCache locationScannerCache,
            Configuration configuration) {
        throw new IllegalStateException("'org.flywaydb.core.internal.scanner.Scanner' is never used in Quarkus");
    }
}
