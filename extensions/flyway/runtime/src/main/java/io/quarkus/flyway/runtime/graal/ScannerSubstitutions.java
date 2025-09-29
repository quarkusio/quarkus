package io.quarkus.flyway.runtime.graal;

import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.internal.scanner.Scanner;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Needed to get rid of some Android related classes
 */
@TargetClass(Scanner.class)
public final class ScannerSubstitutions<I> {

    @Substitute
    public ScannerSubstitutions(final Class<? extends I> implementedInterface,
            final Configuration configuration,
            final Location[] locations) {
        throw new IllegalStateException("'org.flywaydb.core.internal.scanner.Scanner' is never used in Quarkus");
    }
}
