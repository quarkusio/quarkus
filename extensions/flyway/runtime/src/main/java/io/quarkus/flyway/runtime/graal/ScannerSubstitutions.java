package io.quarkus.flyway.runtime.graal;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.flywaydb.core.api.Location;
import org.flywaydb.core.internal.resource.LoadableResource;
import org.flywaydb.core.internal.scanner.ResourceNameCache;
import org.flywaydb.core.internal.scanner.classpath.ResourceAndClassScanner;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * This substitution replaces the Flyway dynamic scanners with a fixed path scanner in native mode
 */
@TargetClass(className = "org.flywaydb.core.internal.scanner.Scanner")
public final class ScannerSubstitutions {

    @Alias
    private List<LoadableResource> resources = new ArrayList<>();

    @Alias
    private List<Class<?>> classes = new ArrayList<>();

    /**
     * Creates only {@link QuarkusPathLocationScanner} instances.
     * Replaces the original method that tries to detect migrations using reflection techniques that are not allowed
     * in native mode
     *
     * @see org.flywaydb.core.internal.scanner.Scanner#Scanner(Class, Collection, ClassLoader, Charset)
     */
    @Substitute
    public ScannerSubstitutions(Class<?> implementedInterface, Collection<Location> locations, ClassLoader classLoader,
            Charset encoding, ResourceNameCache resourceNameCache) {
        ResourceAndClassScanner quarkusScanner = new QuarkusPathLocationScanner();
        resources.addAll(quarkusScanner.scanForResources());
        classes.addAll(quarkusScanner.scanForClasses());
    }
}
